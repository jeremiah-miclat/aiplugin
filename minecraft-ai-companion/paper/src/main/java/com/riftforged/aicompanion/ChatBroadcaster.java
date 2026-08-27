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
    public static final String DEFAULT_BOT_NAME = "mcAi";
    public static final int MC_CHAT_LIMIT = 256;

    // Both set once per (re)load from AiCompanionPlugin — a static config point rather than
    // threading extra parameters through every AskProcessor call site, since this is the one
    // place a fully-built chat line already exists right before it goes out, same shape as how
    // Fabric's GameBridge.sendChat implementation owns this instead.
    private static volatile DiscordWebhook discordWebhook = null;
    private static volatile String botName = DEFAULT_BOT_NAME;

    public static void configureDiscordWebhook(DiscordWebhook webhook) {
        discordWebhook = webhook;
    }

    public static void configureBotName(String name) {
        botName = (name == null || name.isBlank()) ? DEFAULT_BOT_NAME : name;
    }

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
        if (discordWebhook != null) {
            discordWebhook.send(safe);
        }
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
        return Component.text(botName, NamedTextColor.BLUE, TextDecoration.BOLD)
            .append(Component.text(" > ", NamedTextColor.GRAY))
            .append(Component.text(safe, NamedTextColor.WHITE));
    }
}
