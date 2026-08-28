package com.riftforged.aicompanion.fabric;

import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.suggestion.Suggestions;
import com.mojang.brigadier.suggestion.SuggestionsBuilder;
import com.riftforged.aicompanion.AskProcessor;
import com.riftforged.aicompanion.DiscordWebhook;
import com.riftforged.aicompanion.Messages;
import com.riftforged.aicompanion.YamlBotConfig;
import com.riftforged.aicompanion.ai.AiClient;
import com.riftforged.aicompanion.config.AdminConfigCommands;
import com.riftforged.aicompanion.config.ConfigEditException;
import com.riftforged.aicompanion.config.ConfigFileEditor;
import com.riftforged.aicompanion.kb.KnowledgeBase;
import com.riftforged.aicompanion.state.AskQueue;
import com.riftforged.aicompanion.state.ConversationMemory;
import com.riftforged.aicompanion.state.RateLimiter;
import com.riftforged.aicompanion.state.StateStore;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.fabricmc.fabric.api.entity.event.v1.ServerPlayerEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.message.v1.ServerMessageEvents;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Entry point — same structure as the fabric-1.21.11 module's AiCompanionMod (see its Javadoc for
 * the rationale on the batch window not being tick-tied); the two modules read identically at
 * this level since Fabric API's own event/interface names don't change between mapping sets, only
 * the vanilla Minecraft types flowing through their callbacks do (ServerPlayer vs
 * ServerPlayerEntity, Component vs Text, etc. — see FabricGameBridge for those).
 */
public final class AiCompanionMod implements ModInitializer {
    private static final Logger LOGGER = Logger.getLogger("AiCompanion");
    private static final Path CONFIG_DIR = FabricLoader.getInstance().getConfigDir().resolve("aicompanion");

    private YamlBotConfig config;
    private AskQueue askQueue;
    private RateLimiter rateLimiter;
    private ConversationMemory memory;
    private StateStore stateStore;
    private AiClient aiClient;
    private AskProcessor askProcessor;
    private FabricGameBridge bridge;
    private Messages messages;

    private final ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor(r -> {
        Thread t = new Thread(r, "ai-companion-window");
        t.setDaemon(true);
        return t;
    });
    private ScheduledFuture<?> windowTask;
    private MinecraftServer server;

    @Override
    public void onInitialize() {
        this.askQueue = new AskQueue();
        this.rateLimiter = new RateLimiter();
        this.memory = new ConversationMemory();

        ServerLifecycleEvents.SERVER_STARTED.register(server -> {
            this.server = server;
            this.stateStore = new StateStore(CONFIG_DIR.resolve("state.json"), LOGGER);

            StateStore.Data saved = stateStore.load();
            rateLimiter.restore(saved.requestLog());
            askQueue.restore(saved.pendingAsks());

            reload();

            if (!askQueue.isEmpty()) {
                LOGGER.info("[ai-companion] resuming pending question(s) from before restart");
                runAskWindow();
            }
        });

        ServerLifecycleEvents.SERVER_STOPPING.register(server -> {
            if (windowTask != null) windowTask.cancel(false);
            scheduler.shutdownNow();
            if (aiClient != null) aiClient.shutdown();
            persistState();
        });

        ServerPlayerEvents.JOIN.register(player ->
            askProcessor.handleJoin(player.getName().getString()));

        ServerMessageEvents.ALLOW_CHAT_MESSAGE.register((message, sender, params) -> {
            String text = message.signedContent().trim();
            String prefix = config.askPrefix();
            if (!text.regionMatches(true, 0, prefix, 0, prefix.length())) return true;
            String question = text.substring(prefix.length()).trim();
            if (question.isEmpty()) return true;

            String player = sender.getName().getString();
            AskQueue.EnqueueResult result = askQueue.enqueue(player, question, config.maxAsksPerWindow(), config.askCooldownMs());
            return switch (result) {
                case ACCEPTED -> true;
                case DUPLICATE, COOLDOWN -> {
                    if (askQueue.shouldNotifyRejection(player)) {
                        bridge.sendPrivate(player, messages.cooldownNotice(config.askCooldownMs() / 1000));
                    }
                    yield false;
                }
                case FULL -> {
                    if (askQueue.shouldNotifyRejection(player)) {
                        bridge.sendPrivate(player, messages.queueFullNotice());
                    }
                    yield false;
                }
            };
        });

        CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) ->
            dispatcher.register(Commands.literal("aicompanion")
                // Mojang's mapped equivalent of Yarn's CommandManager.requirePermissionLevel(
                // CommandManager.ADMINS_CHECK) - same underlying permission-level overhaul,
                // different method/constant names between the two mapping sets.
                .requires(Commands.hasPermission(Commands.LEVEL_ADMINS))
                .then(Commands.literal("reload").executes(ctx -> {
                    try {
                        reload();
                        ctx.getSource().sendSuccess(() -> Component.literal("[AiCompanion] Reloaded config.yml, server-info.md, and kb/."), false);
                    } catch (RuntimeException e) {
                        LOGGER.log(Level.SEVERE, "[ai-companion] reload failed", e);
                        ctx.getSource().sendFailure(Component.literal("[AiCompanion] Reload failed — check the server console for details."));
                    }
                    return 1;
                }))
                .then(Commands.literal("config").executes(ctx -> {
                    for (String line : AdminConfigCommands.listLines(configFile())) {
                        ctx.getSource().sendSuccess(() -> Component.literal("[AiCompanion] " + line), false);
                    }
                    return 1;
                }))
                .then(Commands.literal("get")
                    .then(Commands.argument("key", StringArgumentType.word())
                        .suggests((ctx, builder) -> suggestKeys(builder))
                        .executes(ctx -> {
                            try {
                                String line = AdminConfigCommands.get(configFile(), StringArgumentType.getString(ctx, "key"));
                                ctx.getSource().sendSuccess(() -> Component.literal("[AiCompanion] " + line), false);
                            } catch (ConfigEditException e) {
                                ctx.getSource().sendFailure(Component.literal("[AiCompanion] " + e.getMessage()));
                            }
                            return 1;
                        })))
                .then(Commands.literal("set")
                    .then(Commands.argument("key", StringArgumentType.word())
                        .suggests((ctx, builder) -> suggestKeys(builder))
                        .then(Commands.argument("value", StringArgumentType.greedyString())
                            .executes(ctx -> {
                                String key = StringArgumentType.getString(ctx, "key");
                                String value = StringArgumentType.getString(ctx, "value");
                                try {
                                    String result = AdminConfigCommands.set(configFile(), key, value);
                                    reload();
                                    ctx.getSource().sendSuccess(() -> Component.literal("[AiCompanion] " + result), false);
                                } catch (ConfigEditException e) {
                                    ctx.getSource().sendFailure(Component.literal("[AiCompanion] " + e.getMessage()));
                                }
                                return 1;
                            }))))
                .then(Commands.literal("models")
                    .then(Commands.literal("list").executes(ctx -> {
                        ctx.getSource().sendSuccess(() -> Component.literal(
                            "[AiCompanion] ai.models: " + String.join(", ", ConfigFileEditor.getModels(configFile()))), false);
                        return 1;
                    }))
                    .then(Commands.literal("add")
                        .then(Commands.argument("id", StringArgumentType.string())
                            .executes(ctx -> {
                                String id = StringArgumentType.getString(ctx, "id");
                                try {
                                    ConfigFileEditor.addModel(configFile(), id);
                                    reload();
                                    ctx.getSource().sendSuccess(() -> Component.literal("[AiCompanion] Added model: " + id), false);
                                } catch (ConfigEditException e) {
                                    ctx.getSource().sendFailure(Component.literal("[AiCompanion] " + e.getMessage()));
                                }
                                return 1;
                            })))
                    .then(Commands.literal("remove")
                        .then(Commands.argument("id", StringArgumentType.string())
                            .executes(ctx -> {
                                String id = StringArgumentType.getString(ctx, "id");
                                try {
                                    ConfigFileEditor.removeModel(configFile(), id);
                                    reload();
                                    ctx.getSource().sendSuccess(() -> Component.literal("[AiCompanion] Removed model: " + id), false);
                                } catch (ConfigEditException e) {
                                    ctx.getSource().sendFailure(Component.literal("[AiCompanion] " + e.getMessage()));
                                }
                                return 1;
                            }))))));
    }

    private static Path configFile() {
        return CONFIG_DIR.resolve("config.yml");
    }

    private static CompletableFuture<Suggestions> suggestKeys(SuggestionsBuilder builder) {
        for (String key : AdminConfigCommands.suggestKeys(builder.getRemaining())) {
            builder.suggest(key);
        }
        return builder.buildFuture();
    }

    /** (Re)builds everything derived from config.yml/server-info.md/kb/ — used by both server
     *  start and /aicompanion reload. Deliberately doesn't touch askQueue/rateLimiter/memory. */
    private void reload() {
        try {
            Files.createDirectories(CONFIG_DIR);
            saveResourceIfMissing("config.yml", CONFIG_DIR.resolve("config.yml"));
            saveResourceIfMissing("server-info.md", CONFIG_DIR.resolve("server-info.md"));
            ensureKbDirSeeded();
        } catch (IOException e) {
            throw new IllegalStateException("Failed to seed config directory " + CONFIG_DIR, e);
        }

        this.config = new YamlBotConfig(CONFIG_DIR.resolve("config.yml"));
        if (config.aiApiKey() == null || config.aiApiKey().isBlank()) {
            LOGGER.warning("[ai-companion] ai.apiKey is not set in config.yml — every AI call will "
                + "fail until an admin fills in a real API key for ai.provider (" + config.aiProvider() + ").");
        }

        DiscordWebhook discordWebhook = new DiscordWebhook(config.discordWebhookUrl(), config.discordUsername(), LOGGER);
        this.bridge = new FabricGameBridge(() -> server, LOGGER, discordWebhook, config.botName(),
            config.chatStyleNameColor(), config.chatStyleNameBold(),
            config.chatStyleMessageColor(), config.chatStyleMessageBold());

        this.messages = new Messages(config.personality(), config);
        KnowledgeBase kb = new KnowledgeBase(CONFIG_DIR.resolve("kb"), CONFIG_DIR.resolve("server-info.md"));

        AiClient oldClient = this.aiClient;
        this.aiClient = new AiClient(LOGGER, config.aiProvider(), config.aiApiKey(), config.aiBaseUrl(), config.aiModels());
        if (oldClient != null) oldClient.shutdown();

        this.askProcessor = new AskProcessor(LOGGER, config, messages, kb, aiClient, memory, rateLimiter, bridge);

        if (windowTask != null) windowTask.cancel(false);
        long periodMs = Math.max(1000, config.batchWindowMs());
        long initialDelayMs = Math.max(0, periodMs - (System.currentTimeMillis() % periodMs));
        this.windowTask = scheduler.scheduleAtFixedRate(this::runAskWindow, initialDelayMs, periodMs, TimeUnit.MILLISECONDS);

        LOGGER.info("[ai-companion] (re)loaded — answering \"" + config.askPrefix()
            + "\" questions every " + (config.batchWindowMs() / 1000) + "s");
    }

    private void runAskWindow() {
        List<AskQueue.AskJob> batch = askQueue.tryBeginWindow();
        if (batch == null) return;
        persistState();
        askProcessor.handleAskBatch(batch)
            .exceptionally(e -> {
                LOGGER.log(Level.WARNING, "[ai-companion] error handling ask batch", e);
                return null;
            })
            .whenComplete((v, e) -> {
                askQueue.endWindow();
                persistState();
            });
    }

    private void persistState() {
        if (stateStore == null) return;
        stateStore.save(new StateStore.Data(rateLimiter.snapshot(), askQueue.snapshotPending()));
    }

    private void saveResourceIfMissing(String resourceName, Path target) throws IOException {
        if (Files.exists(target)) return;
        try (InputStream in = AiCompanionMod.class.getClassLoader().getResourceAsStream(resourceName)) {
            if (in == null) throw new IOException("Missing bundled resource: " + resourceName);
            try (OutputStream out = Files.newOutputStream(target)) {
                in.transferTo(out);
            }
        }
    }

    /** Seeds kb/ with a format-documenting starter file ONLY if the folder doesn't exist yet or
     *  is still empty — never overwrites an admin's real knowledge-base files. */
    private void ensureKbDirSeeded() throws IOException {
        Path kbDir = CONFIG_DIR.resolve("kb");
        Files.createDirectories(kbDir);
        try (var stream = Files.list(kbDir)) {
            if (stream.findAny().isEmpty()) {
                saveResourceIfMissing("kb/01-overview.md", kbDir.resolve("01-overview.md"));
            }
        }
    }
}
