package com.riftforged.aicompanion;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

/** Shared chat-formatting constants/logic — ports MC_CHAT_LIMIT/BOT_NAME/packLines from Paper's
 *  ChatBroadcaster so both Fabric modules (and AskProcessor here in common) use identical values. */
public final class ChatFormat {
    /** Fallback only — the actual display name is configurable via config.yml's "botName" key
     *  (see YamlBotConfig/BotConfig); this is what's used if that's ever unset or blank. */
    public static final String DEFAULT_BOT_NAME = "mcAi";
    public static final int MC_CHAT_LIMIT = 256;

    // Defaults for config.yml's chatStyle section — reproduce the styling that was hardcoded
    // before chat styling became configurable, so an absent/partial section is non-breaking.
    public static final String DEFAULT_NAME_COLOR = "blue";
    public static final boolean DEFAULT_NAME_BOLD = true;
    public static final String DEFAULT_MESSAGE_COLOR = "white";
    public static final boolean DEFAULT_MESSAGE_BOLD = false;

    private static final Pattern HEX_COLOR = Pattern.compile("(?i)^#[0-9a-f]{6}$");

    private ChatFormat() {}

    /** True if raw looks like a "#RRGGBB" hex color (case-insensitive). Kept platform-agnostic
     *  (no TextColor/Formatting type here) since each loader module maps this into its own
     *  color type. */
    public static boolean isHexColor(String raw) {
        return raw != null && HEX_COLOR.matcher(raw.trim()).matches();
    }

    /** Parses a validated (isHexColor()==true) "#RRGGBB" string into a 0xRRGGBB int. */
    public static int parseHexColor(String raw) {
        return Integer.parseInt(raw.trim().substring(1), 16);
    }

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
