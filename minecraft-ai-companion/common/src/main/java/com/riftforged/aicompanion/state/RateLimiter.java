package com.riftforged.aicompanion.state;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Per-key (IP, falling back to "name:<player>") sliding-window rate limit — ports
 * tryConsumeRequest from watcher.js. Only item-give requests spend a slot; plain questions are
 * unlimited (see AskQueue for the batching/cooldown limits that keep plain-question volume in
 * check instead). Persisted via snapshot()/restore() so limits survive a plugin/server restart.
 */
public final class RateLimiter {
    private static final long WINDOW_MS = 24L * 60 * 60 * 1000;

    private final Map<String, List<Long>> requestLog = new HashMap<>();

    public record Result(boolean ok, int remaining) {}

    /** remaining is how many requests are left AFTER this one. */
    public synchronized Result tryConsume(String key, int maxPerWindow) {
        long now = System.currentTimeMillis();
        List<Long> timestamps = requestLog.computeIfAbsent(key, k -> new ArrayList<>());
        timestamps.removeIf(t -> now - t >= WINDOW_MS);
        if (timestamps.size() >= maxPerWindow) {
            return new Result(false, 0);
        }
        timestamps.add(now);
        return new Result(true, maxPerWindow - timestamps.size());
    }

    public synchronized Map<String, List<Long>> snapshot() {
        long now = System.currentTimeMillis();
        // Also prunes requestLog itself, not just the returned copy — otherwise a key whose
        // window has fully expired (player/IP never seen again) sits in memory forever, since
        // tryConsume() only trims a key's own list on its next call and a key that's gone for
        // good never gets one.
        requestLog.entrySet().removeIf(entry -> {
            entry.getValue().removeIf(t -> now - t >= WINDOW_MS);
            return entry.getValue().isEmpty();
        });
        Map<String, List<Long>> out = new HashMap<>();
        for (var entry : requestLog.entrySet()) {
            out.put(entry.getKey(), new ArrayList<>(entry.getValue()));
        }
        return out;
    }

    public synchronized void restore(Map<String, List<Long>> saved) {
        requestLog.clear();
        if (saved == null) return;
        long now = System.currentTimeMillis();
        for (var entry : saved.entrySet()) {
            List<Long> live = new ArrayList<>(entry.getValue());
            live.removeIf(t -> now - t >= WINDOW_MS);
            if (!live.isEmpty()) requestLog.put(entry.getKey(), live);
        }
    }
}
