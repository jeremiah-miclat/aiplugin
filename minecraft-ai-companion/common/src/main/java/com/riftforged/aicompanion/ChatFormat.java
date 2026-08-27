package com.riftforged.aicompanion;

import java.util.ArrayList;
import java.util.List;

/** Shared chat-formatting constants/logic — ports MC_CHAT_LIMIT/BOT_NAME/packLines from Paper's
 *  ChatBroadcaster so both Fabric modules (and AskProcessor here in common) use identical values. */
public final class ChatFormat {
    public static final String BOT_NAME = "mcAi";
    public static final int MC_CHAT_LIMIT = 256;

    private ChatFormat() {}

    /** Greedily packs "Player: text" lines into as few messages as possible under maxLen. */
    public static List<String> packLines(List<String> lines, int maxLen) {
        List<String> packed = new ArrayList<>();
        StringBuilder current = new StringBuilder();
        for (String line : lines) {
            String candidate = current.isEmpty() ? line : current + "  " + line;
            if (!current.isEmpty() && candidate.length() > maxLen) {
                packed.add(current.toString());
                current = new StringBuilder(line);
            } else {
                current = new StringBuilder(candidate);
            }
        }
        if (!current.isEmpty()) packed.add(current.toString());
        return packed;
    }
}
