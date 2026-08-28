package com.riftforged.aicompanion.config;

import java.util.List;

/**
 * Describes one admin-editable scalar key in config.yml — its dotted path (e.g.
 * "chatStyle.nameColor"), its type (for validation), and for ENUM/COLOR the allowed values, plus a
 * one-line description reused in "/aicompanion config" output and command feedback. Built once as a
 * static list in {@link ConfigFieldRegistry}; edited via {@link ConfigFileEditor}.
 *
 * <p>Mirrors common's com.riftforged.aicompanion.config.ConfigField — duplicated here rather than
 * shared since paper/ doesn't depend on the common/ module (see common/pom.xml).
 */
public final class ConfigField {
    public enum Type { STRING, INT, LONG, BOOLEAN, ENUM, COLOR }

    /** Standard Minecraft chat color names — matches config.yml's chatStyle comment. */
    public static final List<String> NAMED_COLORS = List.of(
        "black", "dark_blue", "dark_green", "dark_aqua", "dark_red", "dark_purple", "gold", "gray",
        "dark_gray", "blue", "green", "aqua", "red", "light_purple", "yellow", "white");

    private final String dottedPath;
    private final String leafKey;
    private final Type type;
    private final List<String> allowedValues;
    private final String description;

    public ConfigField(String dottedPath, Type type, List<String> allowedValues, String description) {
        this.dottedPath = dottedPath;
        int lastDot = dottedPath.lastIndexOf('.');
        this.leafKey = lastDot >= 0 ? dottedPath.substring(lastDot + 1) : dottedPath;
        this.type = type;
        this.allowedValues = allowedValues;
        this.description = description;
    }

    public String dottedPath() { return dottedPath; }
    public String leafKey() { return leafKey; }
    public Type type() { return type; }
    public List<String> allowedValues() { return allowedValues; }
    public String description() { return description; }
}
