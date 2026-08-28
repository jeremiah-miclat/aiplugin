package com.riftforged.aicompanion.neoforge;

import com.riftforged.aicompanion.AskProcessor;
import com.riftforged.aicompanion.DiscordWebhook;
import com.riftforged.aicompanion.Messages;
import com.riftforged.aicompanion.Persona;
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
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.loading.FMLPaths;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.RegisterCommandsEvent;
import net.neoforged.neoforge.event.ServerChatEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.server.ServerStartedEvent;
import net.neoforged.neoforge.event.server.ServerStoppingEvent;

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
 * Entry point - NeoForge equivalent of AiCompanionPlugin/AiCompanionMod (Fabric). Ports the same
 * top-level wiring (state load/save, the clock-aligned batch-window scheduler, resuming pending
 * asks) but the batch window runs on its own dedicated ScheduledExecutorService rather than a
 * server-tick event: none of AskProcessor's own logic touches world/player state directly (only
 * via NeoForgeGameBridge, which self-dispatches back onto the main thread via server.execute(...)),
 * same reasoning as the Fabric module.
 */
@Mod("aicompanion")
public final class AiCompanionMod {
    private static final Logger LOGGER = Logger.getLogger("AiCompanion");
    private static final Path CONFIG_DIR = FMLPaths.CONFIGDIR.get().resolve("aicompanion");

    private YamlBotConfig config;
    private AskQueue askQueue;
    private RateLimiter rateLimiter;
    private ConversationMemory memory;
    private StateStore stateStore;
    private AiClient aiClient;
    private AskProcessor askProcessor;
    private NeoForgeGameBridge bridge;
    private Messages messages;

    private final ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor(r -> {
        Thread t = new Thread(r, "ai-companion-window");
        t.setDaemon(true);
        return t;
    });
    private ScheduledFuture<?> windowTask;
    private MinecraftServer server;

    public AiCompanionMod(IEventBus modBus) {
        this.askQueue = new AskQueue();
        this.rateLimiter = new RateLimiter();
        this.memory = new ConversationMemory();

        NeoForge.EVENT_BUS.addListener((final ServerStartedEvent evt) -> {
            this.server = evt.getServer();
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

        NeoForge.EVENT_BUS.addListener((final ServerStoppingEvent evt) -> {
            if (windowTask != null) windowTask.cancel(false);
            scheduler.shutdownNow();
            if (aiClient != null) aiClient.shutdown();
            persistState();
        });

        NeoForge.EVENT_BUS.addListener((final PlayerEvent.PlayerLoggedInEvent evt) ->
            askProcessor.handleJoin(evt.getEntity().getName().getString()));

        NeoForge.EVENT_BUS.addListener((final ServerChatEvent evt) -> {
            String text = evt.getRawText().trim();
            String prefix = config.askPrefix();
            if (!text.regionMatches(true, 0, prefix, 0, prefix.length())) return;
            String question = text.substring(prefix.length()).trim();
            if (question.isEmpty()) return;

            String player = evt.getUsername();
            AskQueue.EnqueueResult result = askQueue.enqueue(player, question, config.maxAsksPerWindow(), config.askCooldownMs());
            switch (result) {
                case ACCEPTED -> { }
                case DUPLICATE, COOLDOWN -> {
                    if (askQueue.shouldNotifyRejection(player)) {
                        bridge.sendPrivate(player, messages.cooldownNotice(config.askCooldownMs() / 1000));
                    }
                    evt.setCanceled(true);
                }
                case FULL -> {
                    if (askQueue.shouldNotifyRejection(player)) {
                        bridge.sendPrivate(player, messages.queueFullNotice());
                    }
                    evt.setCanceled(true);
                }
            }
        });

        NeoForge.EVENT_BUS.addListener((final RegisterCommandsEvent evt) ->
            evt.getDispatcher().register(Commands.literal("aicompanion")
                // 1.21.11 replaced integer op-levels with a named permission-level check; see
                // fabric-26.1's README notes on the same Mojang-mapped API.
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

    /** (Re)builds everything derived from config.yml/server-info.md/kb/ - used by both server
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
        this.bridge = new NeoForgeGameBridge(() -> server, LOGGER, discordWebhook, config.botName());

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
     *  is still empty - never overwrites an admin's real knowledge-base files. */
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
