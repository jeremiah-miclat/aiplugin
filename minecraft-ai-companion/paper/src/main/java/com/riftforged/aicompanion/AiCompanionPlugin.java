package com.riftforged.aicompanion;

import com.riftforged.aicompanion.ai.AiClient;
import com.riftforged.aicompanion.commands.ReloadCommand;
import com.riftforged.aicompanion.kb.KnowledgeBase;
import com.riftforged.aicompanion.listeners.AskListener;
import com.riftforged.aicompanion.listeners.JoinListener;
import com.riftforged.aicompanion.state.AskQueue;
import com.riftforged.aicompanion.state.ConversationMemory;
import com.riftforged.aicompanion.state.RateLimiter;
import com.riftforged.aicompanion.state.StateStore;
import org.bukkit.event.HandlerList;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.util.List;
import java.util.logging.Level;

/**
 * Entry point — ports the top-level wiring at the bottom of watcher.js (state load/save, the
 * clock-aligned batch-window scheduler, and resuming any pending asks left over from a previous
 * run) into Paper's plugin lifecycle. Runs entirely in-process: no RCON, no log tailing — join and
 * chat are handled directly via Bukkit events, and item gives go straight through the console
 * command dispatcher on the main thread.
 */
public final class AiCompanionPlugin extends JavaPlugin {
    private BotConfig config;
    private AskQueue askQueue;
    private RateLimiter rateLimiter;
    private ConversationMemory memory;
    private StateStore stateStore;
    private AiClient aiClient;
    private AskProcessor askProcessor;
    private int windowTaskId = -1;

    @Override
    public void onEnable() {
        // State that must survive a /aicompanion reload (unlike config/messages/askProcessor,
        // which reload() below rebuilds fresh every time) — created once here, not in reload().
        this.askQueue = new AskQueue();
        this.rateLimiter = new RateLimiter();
        this.memory = new ConversationMemory();
        this.stateStore = new StateStore(new File(getDataFolder(), "state.json").toPath(), getLogger());

        StateStore.Data saved = stateStore.load();
        rateLimiter.restore(saved.requestLog());
        askQueue.restore(saved.pendingAsks());

        getCommand("aicompanion").setExecutor(new ReloadCommand(this));

        reload();

        if (!askQueue.isEmpty()) {
            getLogger().info("[ai-companion] resuming pending question(s) from before restart");
            getServer().getScheduler().runTaskAsynchronously(this, this::runAskWindow);
        }
    }

    @Override
    public void onDisable() {
        if (windowTaskId != -1) {
            getServer().getScheduler().cancelTask(windowTaskId);
        }
        if (aiClient != null) {
            aiClient.shutdown();
        }
        persistState();
    }

    /**
     * (Re)builds everything that's derived from config.yml/server-info.md/kb/ and re-registers
     * the listeners that close over them — used both by onEnable (first load) and by
     * /aicompanion reload (picking up edited files without a server restart). Deliberately does
     * NOT touch askQueue/rateLimiter/memory: those hold live state (pending asks, 24h item-give
     * cooldowns, conversation history) that a reload should never discard.
     */
    public void reload() {
        saveDefaultConfig();
        reloadConfig();
        saveResourceIfMissing("server-info.md");
        ensureKbDirSeeded();

        this.config = new BotConfig(getConfig());
        if (config.aiApiKey() == null || config.aiApiKey().isBlank()) {
            getLogger().warning("[ai-companion] ai.apiKey is not set in config.yml — "
                + "every AI call will fail until an admin fills in a real API key for ai.provider ("
                + config.aiProvider() + ").");
        }

        Messages messages = new Messages(config.personality(), config);
        KnowledgeBase kb = new KnowledgeBase(
            new File(getDataFolder(), "kb").toPath(),
            new File(getDataFolder(), "server-info.md").toPath());

        AiClient oldClient = this.aiClient;
        this.aiClient = new AiClient(getLogger(), config.aiProvider(), config.aiApiKey(), config.aiBaseUrl(), config.aiModels());
        if (oldClient != null) {
            oldClient.shutdown();
        }

        this.askProcessor = new AskProcessor(this, config, messages, kb, aiClient, memory, rateLimiter);

        // Listeners hold final references to config/messages/askProcessor, so they can't just be
        // patched in place — drop and re-register them with the freshly built ones.
        HandlerList.unregisterAll(this);
        getServer().getPluginManager().registerEvents(new JoinListener(this, askProcessor), this);
        getServer().getPluginManager().registerEvents(new AskListener(this, config, messages, askQueue), this);

        if (windowTaskId != -1) {
            getServer().getScheduler().cancelTask(windowTaskId);
        }
        long periodTicks = Math.max(1, config.batchWindowMs() / 50);
        long initialDelayTicks = Math.max(1, (config.batchWindowMs() - (System.currentTimeMillis() % config.batchWindowMs())) / 50);
        this.windowTaskId = getServer().getScheduler().runTaskTimerAsynchronously(
            this, this::runAskWindow, initialDelayTicks, periodTicks).getTaskId();

        getLogger().info("[ai-companion] (re)loaded — answering \"" + config.askPrefix()
            + "\" questions every " + (config.batchWindowMs() / 1000) + "s");
    }

    private void runAskWindow() {
        List<AskQueue.AskJob> batch = askQueue.tryBeginWindow();
        if (batch == null) return;
        persistState(); // so a crash mid-call doesn't lose these questions
        askProcessor.handleAskBatch(batch)
            .exceptionally(e -> {
                getLogger().log(Level.WARNING, "[ai-companion] error handling ask batch", e);
                return null;
            })
            .whenComplete((v, e) -> {
                askQueue.endWindow();
                persistState();
            });
    }

    private void persistState() {
        stateStore.save(new StateStore.Data(rateLimiter.snapshot(), askQueue.snapshotPending()));
    }

    private void saveResourceIfMissing(String name) {
        File target = new File(getDataFolder(), name);
        if (!target.exists()) {
            saveResource(name, false);
        }
    }

    /**
     * Seeds kb/ with a format-documenting starter file ONLY if the folder doesn't exist yet or is
     * still empty — never overwrites an admin's real knowledge-base files. Every server's KB
     * content is different, so this ships as a generic template to fill in, not fixed lore.
     */
    private void ensureKbDirSeeded() {
        File kbDir = new File(getDataFolder(), "kb");
        String[] existing = kbDir.list();
        boolean empty = !kbDir.exists() || existing == null || existing.length == 0;
        if (empty) {
            saveResource("kb/01-overview.md", false);
        }
    }
}
