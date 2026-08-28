package com.riftforged.aicompanion.forge;

import com.riftforged.aicompanion.AskProcessor;
import com.riftforged.aicompanion.DiscordWebhook;
import com.riftforged.aicompanion.Messages;
import com.riftforged.aicompanion.YamlBotConfig;
import com.riftforged.aicompanion.ai.AiClient;
import com.riftforged.aicompanion.kb.KnowledgeBase;
import com.riftforged.aicompanion.state.AskQueue;
import com.riftforged.aicompanion.state.ConversationMemory;
import com.riftforged.aicompanion.state.RateLimiter;
import com.riftforged.aicompanion.state.StateStore;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.event.ServerChatEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.event.server.ServerStartedEvent;
import net.minecraftforge.event.server.ServerStoppingEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import net.minecraftforge.fml.loading.FMLPaths;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Entry point — Forge equivalent of AiCompanionPlugin/the Fabric modules' AiCompanionMod. Same
 * top-level wiring (state load/save, the clock-aligned batch-window scheduler, resuming pending
 * asks) on its own dedicated ScheduledExecutorService rather than any tick/event loop: none of
 * AskProcessor's own logic touches world/player state directly (only via ForgeGameBridge, which
 * self-dispatches back onto the main thread via server.execute(...)), so there's no need to tie
 * scheduling to the server tick.
 *
 * Forge's event model differs from Fabric API's: every event type here (ServerStartedEvent,
 * ServerStoppingEvent, PlayerEvent.PlayerLoggedInEvent, ServerChatEvent, RegisterCommandsEvent) is
 * a global, per-event-type static EventBus — "BUS.addListener(...)" — rather than Fabric's
 * per-callback-interface registration, and doesn't need a mod-scoped BusGroup the way mod-lifecycle
 * events (FMLCommonSetupEvent, etc.) do.
 */
@Mod(AiCompanionMod.MODID)
public final class AiCompanionMod {
    public static final String MODID = "aicompanion";
    private static final Logger LOGGER = Logger.getLogger("AiCompanion");
    private static final Path CONFIG_DIR = FMLPaths.CONFIGDIR.get().resolve("aicompanion");

    private YamlBotConfig config;
    private AskQueue askQueue;
    private RateLimiter rateLimiter;
    private ConversationMemory memory;
    private StateStore stateStore;
    private AiClient aiClient;
    private AskProcessor askProcessor;
    private ForgeGameBridge bridge;
    private Messages messages;

    private final ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor(r -> {
        Thread t = new Thread(r, "ai-companion-window");
        t.setDaemon(true);
        return t;
    });
    private ScheduledFuture<?> windowTask;
    private MinecraftServer server;

    public AiCompanionMod(FMLJavaModLoadingContext context) {
        this.askQueue = new AskQueue();
        this.rateLimiter = new RateLimiter();
        this.memory = new ConversationMemory();

        ServerStartedEvent.BUS.addListener(event -> {
            this.server = event.getServer();
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

        ServerStoppingEvent.BUS.addListener(event -> {
            if (windowTask != null) windowTask.cancel(false);
            scheduler.shutdownNow();
            if (aiClient != null) aiClient.shutdown();
            persistState();
        });

        PlayerEvent.PlayerLoggedInEvent.BUS.addListener(event ->
            askProcessor.handleJoin(event.getEntity().getGameProfile().name()));

        // Predicate<ServerChatEvent> — returning true CANCELS the event (suppresses the message),
        // the inverse of Fabric's ALLOW_CHAT_MESSAGE (which returns true to ALLOW it through).
        ServerChatEvent.BUS.addListener(event -> {
            String text = event.getRawText().trim();
            String prefix = config.askPrefix();
            if (!text.regionMatches(true, 0, prefix, 0, prefix.length())) return false;
            String question = text.substring(prefix.length()).trim();
            if (question.isEmpty()) return false;

            String player = event.getUsername();
            AskQueue.EnqueueResult result = askQueue.enqueue(player, question, config.maxAsksPerWindow(), config.askCooldownMs());
            return switch (result) {
                case ACCEPTED -> false;
                case DUPLICATE, COOLDOWN -> {
                    if (askQueue.shouldNotifyRejection(player)) {
                        bridge.sendPrivate(player, messages.cooldownNotice(config.askCooldownMs() / 1000));
                    }
                    yield true;
                }
                case FULL -> {
                    if (askQueue.shouldNotifyRejection(player)) {
                        bridge.sendPrivate(player, messages.queueFullNotice());
                    }
                    yield true;
                }
            };
        });

        RegisterCommandsEvent.BUS.addListener(event ->
            event.getDispatcher().register(Commands.literal("aicompanion")
                // 1.21.11 replaced integer op-levels with a named PermissionCheck; LEVEL_ADMINS is
                // the modern equivalent of the old "requires op level 4".
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
                }))));
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
        this.bridge = new ForgeGameBridge(() -> server, LOGGER, discordWebhook, config.botName());

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
