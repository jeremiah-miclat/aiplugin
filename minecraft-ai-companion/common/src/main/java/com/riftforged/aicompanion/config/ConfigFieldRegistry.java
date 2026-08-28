package com.riftforged.aicompanion.config;

import java.util.List;
import java.util.Map;
import java.util.LinkedHashMap;

import static com.riftforged.aicompanion.config.ConfigField.Type.BOOLEAN;
import static com.riftforged.aicompanion.config.ConfigField.Type.COLOR;
import static com.riftforged.aicompanion.config.ConfigField.Type.ENUM;
import static com.riftforged.aicompanion.config.ConfigField.Type.INT;
import static com.riftforged.aicompanion.config.ConfigField.Type.LONG;
import static com.riftforged.aicompanion.config.ConfigField.Type.STRING;

/**
 * The whitelist of config.yml keys admins can view/edit via "/aicompanion get|set|config".
 * Deliberately excludes "ai.apiKey" (a secret — never echoed into chat/logs) and "ai.models" (a
 * list, handled separately by ConfigFileEditor's getModels/addModel/removeModel and the
 * "/aicompanion models" subcommand rather than as a scalar field here).
 */
public final class ConfigFieldRegistry {
    private static final List<ConfigField> FIELDS = List.of(
        new ConfigField("botName", STRING, null, "Name shown in chat before every bot message."),
        new ConfigField("chatStyle.nameColor", COLOR, ConfigField.NAMED_COLORS, "Color of the bot's name in chat."),
        new ConfigField("chatStyle.nameBold", BOOLEAN, null, "Whether the bot's name is bold."),
        new ConfigField("chatStyle.messageColor", COLOR, ConfigField.NAMED_COLORS, "Color of the bot's message text."),
        new ConfigField("chatStyle.messageBold", BOOLEAN, null, "Whether the bot's message text is bold."),
        new ConfigField("serverName", STRING, null, "Server name mentioned by the bot."),
        new ConfigField("personality", ENUM, List.of("friendly", "trashtalk"), "Bot personality."),
        new ConfigField("askPrefix", STRING, null, "Chat prefix that triggers a question (e.g. \"!ai\")."),
        new ConfigField("batchWindowMs", LONG, null, "Milliseconds between AI call batches."),
        new ConfigField("maxAsksPerWindow", INT, null, "Max accepted questions per batch window."),
        new ConfigField("askCooldownSeconds", INT, null, "Minimum seconds between one player's accepted asks."),
        new ConfigField("maxAskSubparts", INT, null, "Max distinct requests one \"!ai\" message can resolve."),
        new ConfigField("itemGiving.enabled", BOOLEAN, null, "Master switch for the bot giving out items."),
        new ConfigField("itemGiving.maxPerDay", INT, null, "Max item-give requests fulfilled per player per 24h."),
        new ConfigField("itemGiving.maxQuantity", INT, null, "Max quantity for ordinary stackable items."),
        new ConfigField("itemGiving.maxEquipmentQuantity", INT, null, "Max quantity for equipment items."),
        new ConfigField("broadcastReplies", BOOLEAN, null, "Whether bot chat lines broadcast server-wide."),
        new ConfigField("ai.provider", ENUM, List.of("openrouter", "openai-compatible", "anthropic"), "Which AI backend to talk to."),
        new ConfigField("ai.baseUrl", STRING, null, "Base URL for the \"openai-compatible\" provider."),
        new ConfigField("discord.webhookUrl", STRING, null, "Discord webhook URL to mirror bot messages into."),
        new ConfigField("discord.username", STRING, null, "Override name Discord shows for mirrored messages.")
    );

    private static final Map<String, ConfigField> BY_PATH = new LinkedHashMap<>();
    static {
        for (ConfigField field : FIELDS) {
            BY_PATH.put(field.dottedPath().toLowerCase(), field);
        }
    }

    private ConfigFieldRegistry() {}

    public static List<ConfigField> all() {
        return FIELDS;
    }

    /** Case-insensitive lookup by dotted path, or null if unknown/not editable (e.g. "ai.apiKey"). */
    public static ConfigField find(String dottedPath) {
        return dottedPath == null ? null : BY_PATH.get(dottedPath.toLowerCase());
    }
}
