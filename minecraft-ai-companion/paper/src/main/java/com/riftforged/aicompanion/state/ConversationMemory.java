package com.riftforged.aicompanion.state;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Per-player and server-wide conversation memory — ports the conversationHistory/globalHistory
 * logic from watcher.js. In-memory only, like the original (a restart just starts fresh
 * conversations); no AI call is spent maintaining this, it's just pasted into the next prompt.
 */
public final class ConversationMemory {
    private static final int CONVERSATION_TURNS = 10;
    private static final int GLOBAL_HISTORY_SIZE = 100;
    private static final int GLOBAL_CONTEXT_WINDOW = 12;

    public record Turn(String question, String reply) {}
    public record GlobalTurn(String player, String question, String reply) {}

    private final Map<String, Deque<Turn>> perPlayer = new HashMap<>();
    private final Deque<GlobalTurn> global = new ArrayDeque<>();

    public synchronized List<Turn> getHistory(String player) {
        Deque<Turn> h = perPlayer.get(player.toLowerCase());
        return h == null ? List.of() : new ArrayList<>(h);
    }

    public synchronized void appendHistory(String player, String question, String reply) {
        String key = player.toLowerCase();
        Deque<Turn> h = perPlayer.computeIfAbsent(key, k -> new ArrayDeque<>());
        h.addLast(new Turn(question, reply));
        while (h.size() > CONVERSATION_TURNS) h.pollFirst();
    }

    public synchronized void appendGlobalHistory(String player, String question, String reply) {
        global.addLast(new GlobalTurn(player, question, reply));
        while (global.size() > GLOBAL_HISTORY_SIZE) global.pollFirst();
    }

    public static String formatHistory(List<Turn> history) {
        if (history.isEmpty()) return "";
        StringBuilder sb = new StringBuilder(
            "Recent conversation with this player, oldest first (for context/continuity only — " +
            "resolve just the CURRENT message below, don't re-answer these):\n");
        for (int i = 0; i < history.size(); i++) {
            Turn t = history.get(i);
            if (i > 0) sb.append("\n");
            sb.append("Player: ").append(t.question()).append("\nYou: ").append(t.reply());
        }
        return sb.toString();
    }

    public synchronized String formatGlobalContext() {
        if (global.isEmpty()) return "";
        List<GlobalTurn> all = new ArrayList<>(global);
        List<GlobalTurn> recent = all.subList(Math.max(0, all.size() - GLOBAL_CONTEXT_WINDOW), all.size());
        StringBuilder sb = new StringBuilder(
            "Recent server chat log, oldest first (background context only, shared across all " +
            "players — you may naturally reference something someone said, but do NOT answer or " +
            "address anyone here, only whoever is asking in the CURRENT message(s) below):\n");
        for (int i = 0; i < recent.size(); i++) {
            GlobalTurn t = recent.get(i);
            if (i > 0) sb.append("\n");
            sb.append(t.player()).append(": ").append(t.question()).append("\nYou: ").append(t.reply());
        }
        return sb.toString();
    }
}
