package com.riftforged.aicompanion;

import com.riftforged.aicompanion.ai.AiProvider;

import java.nio.file.Path;
import java.util.List;

/**
 * Typed view over config.yml for the Fabric modules — same shape/keys/defaults as Paper's
 * BotConfig (which reads the identical file format via Bukkit's FileConfiguration instead), so a
 * config.yml is portable between platforms. Shared between fabric-1.21.11 and fabric-26.1 since
 * config.yml's format doesn't vary by Minecraft version.
 */
public final class YamlBotConfig implements BotSettings {
    private final String botName;
    private final String serverName;
    private final String personality;
    private final String askPrefix;
    private final long batchWindowMs;
    private final int maxAsksPerWindow;
    private final long askCooldownMs;
    private final int maxAskSubparts;
    private final boolean itemGivingEnabled;
    private final int itemGivingMaxPerDay;
    private final int itemGivingMaxQuantity;
    private final int itemGivingMaxEquipmentQuantity;
    private final AiProvider aiProvider;
    private final String aiApiKey;
    private final String aiBaseUrl;
    private final List<String> aiModels;
    private final boolean broadcastReplies;
    private final String discordWebhookUrl;
    private final String discordUsername;
    private final String chatStyleNameColor;
    private final boolean chatStyleNameBold;
    private final String chatStyleMessageColor;
    private final boolean chatStyleMessageBold;

    public YamlBotConfig(Path configFile) {
        YamlConfig cfg = new YamlConfig(configFile);
        this.botName = cfg.getString("botName", "mcAi");
        this.serverName = cfg.getString("serverName", "Minecraft Server");
        this.personality = cfg.getString("personality", "friendly");
        this.askPrefix = cfg.getString("askPrefix", "!ai");
        this.batchWindowMs = cfg.getLong("batchWindowMs", 10000L);
        this.maxAsksPerWindow = cfg.getInt("maxAsksPerWindow", 10);
        int cooldownSeconds = cfg.getInt("askCooldownSeconds", -1);
        this.askCooldownMs = cooldownSeconds >= 0 ? cooldownSeconds * 1000L : this.batchWindowMs;
        this.maxAskSubparts = Math.max(1, cfg.getInt("maxAskSubparts", 1));
        this.itemGivingEnabled = cfg.getBoolean("itemGiving.enabled", false);
        this.itemGivingMaxPerDay = cfg.getInt("itemGiving.maxPerDay", 10);
        this.itemGivingMaxQuantity = cfg.getInt("itemGiving.maxQuantity", 64);
        this.itemGivingMaxEquipmentQuantity = cfg.getInt("itemGiving.maxEquipmentQuantity", 1);
        this.aiProvider = AiProvider.fromConfig(cfg.getString("ai.provider", "openrouter"));
        this.aiApiKey = cfg.getString("ai.apiKey", "");
        this.aiBaseUrl = cfg.getString("ai.baseUrl", "");
        this.aiModels = cfg.getStringList("ai.models");
        this.broadcastReplies = cfg.getBoolean("broadcastReplies", true);
        this.discordWebhookUrl = cfg.getString("discord.webhookUrl", "");
        this.discordUsername = cfg.getString("discord.username", "");
        this.chatStyleNameColor = cfg.getString("chatStyle.nameColor", ChatFormat.DEFAULT_NAME_COLOR);
        this.chatStyleNameBold = cfg.getBoolean("chatStyle.nameBold", ChatFormat.DEFAULT_NAME_BOLD);
        this.chatStyleMessageColor = cfg.getString("chatStyle.messageColor", ChatFormat.DEFAULT_MESSAGE_COLOR);
        this.chatStyleMessageBold = cfg.getBoolean("chatStyle.messageBold", ChatFormat.DEFAULT_MESSAGE_BOLD);
    }

    public String botName() { return botName; }
    public String serverName() { return serverName; }
    public Persona personality() { return Persona.fromConfig(personality); }
    @Override public String askPrefix() { return askPrefix; }
    public long batchWindowMs() { return batchWindowMs; }
    public int maxAsksPerWindow() { return maxAsksPerWindow; }
    public long askCooldownMs() { return askCooldownMs; }
    public int maxAskSubparts() { return maxAskSubparts; }
    @Override public boolean itemGivingEnabled() { return itemGivingEnabled; }
    @Override public int itemGivingMaxPerDay() { return itemGivingMaxPerDay; }
    public int itemGivingMaxQuantity() { return itemGivingMaxQuantity; }
    public int itemGivingMaxEquipmentQuantity() { return itemGivingMaxEquipmentQuantity; }
    public AiProvider aiProvider() { return aiProvider; }
    public String aiApiKey() { return aiApiKey; }
    public String aiBaseUrl() { return aiBaseUrl; }
    public List<String> aiModels() { return aiModels; }
    public boolean broadcastReplies() { return broadcastReplies; }
    public String discordWebhookUrl() { return discordWebhookUrl; }
    public String discordUsername() { return discordUsername; }
    public String chatStyleNameColor() { return chatStyleNameColor; }
    public boolean chatStyleNameBold() { return chatStyleNameBold; }
    public String chatStyleMessageColor() { return chatStyleMessageColor; }
    public boolean chatStyleMessageBold() { return chatStyleMessageBold; }
}
