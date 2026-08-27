package com.riftforged.aicompanion;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Bukkit;
import org.bukkit.plugin.Plugin;

import java.util.ArrayList;
import java.util.List;

/**
 * Ports sendChat/sendPackedReplies/packLines from watcher.js. Every caller passes a message
 * already shaped as "PlayerName: text", so this only adds the "mcAi > " sender prefix — using
 * Adventure Components directly (Paper's Server implements Audience) instead of a tellraw/RCON
 * round trip, since the plugin runs in-process.
 */
public final class ChatBroadcaster {
    public static final String BOT_NAME = "mcAi";
    public static final int MC_CHAT_LIMIT = 256;

    private ChatBroadcaster() {}

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

    public static void sendPackedReplies(Plugin plugin, List<String> lines, boolean broadcast) {
        for (String message : packLines(lines, MC_CHAT_LIMIT)) {
            sendChat(plugin, message, broadcast);
        }
    }

    public static void sendChat(Plugin plugin, String message, boolean broadcast) {
        String safe = com.riftforged.aicompanion.ai.AiClient.sanitize(message, MC_CHAT_LIMIT);
        plugin.getLogger().info("[ai-companion] -> " + safe);
        Component component = formatted(safe);
        Bukkit.getScheduler().runTask(plugin, () -> {
            if (broadcast) {
                Bukkit.broadcast(component);
            } else {
                Bukkit.getConsoleSender().sendMessage(component);
            }
        });
    }

    /** Sends to just one player (e.g. a spam/cooldown notice) — never broadcast, regardless of
     *  the broadcastReplies setting, since bookkeeping notices like this aren't news to anyone
     *  else. Silently a no-op if the player has since logged off. */
    public static void sendPrivate(Plugin plugin, String playerName, String message) {
        String safe = com.riftforged.aicompanion.ai.AiClient.sanitize(message, MC_CHAT_LIMIT);
        Component component = formatted(safe);
        Bukkit.getScheduler().runTask(plugin, () -> {
            org.bukkit.entity.Player player = Bukkit.getPlayerExact(playerName);
            if (player != null) player.sendMessage(component);
        });
    }

    private static Component formatted(String safe) {
        return Component.text(BOT_NAME, NamedTextColor.BLUE, TextDecoration.BOLD)
            .append(Component.text(" > ", NamedTextColor.GRAY))
            .append(Component.text(safe, NamedTextColor.WHITE));
    }
}
