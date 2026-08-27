package com.riftforged.aicompanion;

/** The handful of config values Messages needs — kept as a small interface (rather than depending
 *  on a concrete config class) purely so Messages can be shared here in common/ without dragging
 *  in a specific config-loading mechanism. Implemented by YamlBotConfig for the Fabric modules. */
public interface BotSettings {
    String askPrefix();
    boolean itemGivingEnabled();
    int itemGivingMaxPerDay();
}
