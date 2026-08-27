package com.riftforged.aicompanion.state;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * The pending "!ask" queue and the ask-window batching state — ports askQueue/currentBatch/
 * duplicateNoticeSent/processing from watcher.js, plus two anti-spam layers not present in the
 * original (which relied entirely on plain questions being unmetered to justify no per-question
 * limit):
 *
 *  - A per-player cooldown independent of the batch window (see enqueue's minAskIntervalMs),
 *    so an admin can slow down a single persistent asker without changing the server-wide
 *    batching cadence.
 *  - Rejection notices (DUPLICATE/COOLDOWN/FULL) are throttled to one broadcast per player per
 *    window, not one per rejected message — otherwise a crowd all hitting a full queue at once
 *    (or one player mashing the same message) turns into a self-inflicted chat-spam storm.
 *
 * Every accepted ask still waits for the next clock-aligned window (see AiCompanionPlugin's
 * scheduler) so a whole window's worth of asks goes out as one AI call — regardless of how many
 * messages come in, at most one AI call happens per window, which is the main thing keeping a
 * spam flood from turning into an API-overload problem (and, for a metered provider, a cost
 * problem).
 */
public final class AskQueue {
    public record AskJob(String player, String text) {}

    public enum EnqueueResult { ACCEPTED, DUPLICATE, COOLDOWN, FULL }

    private final List<AskJob> pending = new ArrayList<>();
    private final List<AskJob> currentBatch = new ArrayList<>();
    private final Set<String> rejectionNoticeSent = new HashSet<>();
    private final Map<String, Long> lastAcceptedAt = new HashMap<>();
    private boolean processing = false;

    public synchronized EnqueueResult enqueue(String player, String text, int maxAsksPerWindow, long minAskIntervalMs) {
        String key = player.toLowerCase();
        boolean alreadyQueued = pending.stream().anyMatch(j -> j.player().equalsIgnoreCase(key));
        if (alreadyQueued) {
            return EnqueueResult.DUPLICATE;
        }
        Long last = lastAcceptedAt.get(key);
        if (last != null && System.currentTimeMillis() - last < minAskIntervalMs) {
            return EnqueueResult.COOLDOWN;
        }
        if (pending.size() >= maxAsksPerWindow) {
            return EnqueueResult.FULL;
        }
        pending.add(new AskJob(player, text));
        lastAcceptedAt.put(key, System.currentTimeMillis());
        return EnqueueResult.ACCEPTED;
    }

    /** True only the first time this is called for a given player within the current window —
     *  covers DUPLICATE, COOLDOWN, and FULL alike, so a player spammed past any one of those
     *  limits gets told once, not once per rejected message. */
    public synchronized boolean shouldNotifyRejection(String player) {
        return rejectionNoticeSent.add(player.toLowerCase());
    }

    public synchronized boolean isEmpty() {
        return pending.isEmpty();
    }

    public synchronized boolean isProcessing() {
        return processing;
    }

    /**
     * Atomically checks-and-drains: returns null (and does nothing) if a window is already being
     * processed or nothing is queued, otherwise drains the queue into currentBatch and marks
     * processing started. Must be a single synchronized call rather than isProcessing()+drain, or
     * two overlapping scheduler ticks (an async repeating task's next tick can start before a slow
     * previous one finishes) could both pass the check before either flips `processing`.
     */
    public synchronized List<AskJob> tryBeginWindow() {
        if (processing || pending.isEmpty()) return null;
        processing = true;
        rejectionNoticeSent.clear();
        pruneStaleCooldowns();
        List<AskJob> batch = new ArrayList<>(pending);
        pending.clear();
        currentBatch.clear();
        currentBatch.addAll(batch);
        return batch;
    }

    /** Bounds lastAcceptedAt's growth on a long-running server with many distinct players — any
     *  entry older than a day is well past any sane cooldown config, so it's just dead weight. */
    private static final long STALE_COOLDOWN_MS = 24L * 60 * 60 * 1000;

    private void pruneStaleCooldowns() {
        long now = System.currentTimeMillis();
        lastAcceptedAt.values().removeIf(t -> now - t >= STALE_COOLDOWN_MS);
    }

    public synchronized void endWindow() {
        currentBatch.clear();
        processing = false;
    }

    /** Everything not yet replied to — mid-call plus still-queued — for crash-safe persistence. */
    public synchronized List<StateStore.PendingAsk> snapshotPending() {
        List<StateStore.PendingAsk> out = new ArrayList<>();
        for (AskJob j : currentBatch) out.add(new StateStore.PendingAsk(j.player(), j.text()));
        for (AskJob j : pending) out.add(new StateStore.PendingAsk(j.player(), j.text()));
        return out;
    }

    public synchronized void restore(List<StateStore.PendingAsk> saved) {
        pending.clear();
        if (saved == null) return;
        for (StateStore.PendingAsk p : saved) pending.add(new AskJob(p.player(), p.text()));
    }
}
