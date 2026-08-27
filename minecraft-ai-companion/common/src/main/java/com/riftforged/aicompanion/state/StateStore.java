package com.riftforged.aicompanion.state;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;

import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Persists rate-limit history and any not-yet-replied-to asks to state.json in the plugin's data
 * folder, so both survive a plugin/server restart — ports loadState/saveState from watcher.js.
 * Per-player/global conversation history is deliberately NOT persisted here, matching the
 * original (in-memory only).
 */
public final class StateStore {
    public record PendingAsk(String player, String text) {}

    public record Data(Map<String, List<Long>> requestLog, List<PendingAsk> pendingAsks) {}

    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final java.lang.reflect.Type DATA_TYPE = new TypeToken<Data>() {}.getType();

    private final Path file;
    private final Logger logger;

    public StateStore(Path file, Logger logger) {
        this.file = file;
        this.logger = logger;
    }

    public Data load() {
        if (!Files.exists(file)) {
            return new Data(Map.of(), List.of());
        }
        try (Reader reader = Files.newBufferedReader(file, StandardCharsets.UTF_8)) {
            Data data = GSON.fromJson(reader, DATA_TYPE);
            if (data == null) return new Data(Map.of(), List.of());
            return new Data(
                data.requestLog() != null ? data.requestLog() : Map.of(),
                data.pendingAsks() != null ? data.pendingAsks() : List.of());
        } catch (IOException | com.google.gson.JsonSyntaxException e) {
            logger.log(Level.WARNING, "[ai-companion] failed to load state.json, starting fresh", e);
            return new Data(Map.of(), List.of());
        }
    }

    public void save(Data data) {
        try {
            Files.createDirectories(file.getParent());
            try (Writer writer = Files.newBufferedWriter(file, StandardCharsets.UTF_8)) {
                GSON.toJson(data, DATA_TYPE, writer);
            }
        } catch (IOException e) {
            logger.log(Level.WARNING, "[ai-companion] failed to save state.json", e);
        }
    }
}
