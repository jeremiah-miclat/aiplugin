package com.riftforged.aicompanion.config;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * Plain-Java formatting/dispatch layer over {@link ConfigFieldRegistry} and {@link
 * ConfigFileEditor}, shared by every platform's "/aicompanion get|set|config|models" command
 * handlers so the actual strings shown to an admin (and the old-value/new-value/error wording) are
 * identical everywhere — only the Brigadier/Bukkit command-tree wiring and the platform's own chat
 * text type differ per module.
 */
public final class AdminConfigCommands {
    private AdminConfigCommands() {}

    /** One "key = value" line per editable field, plus a summary line for the ai.models list. */
    public static List<String> listLines(Path configFile) {
        List<String> lines = new ArrayList<>();
        for (ConfigField field : ConfigFieldRegistry.all()) {
            lines.add(formatLine(field, ConfigFileEditor.get(configFile, field)));
        }
        lines.add("ai.models = " + ConfigFileEditor.getModels(configFile).size()
            + " model(s) configured — see /aicompanion models list");
        return lines;
    }

    /** "key = value" for one field. Throws ConfigEditException for apiKey/models/unknown keys. */
    public static String get(Path configFile, String dottedPath) {
        ConfigField field = resolve(dottedPath);
        return formatLine(field, ConfigFileEditor.get(configFile, field));
    }

    /** Validates + writes the new value, returning a human-readable "key: old -> new" summary. */
    public static String set(Path configFile, String dottedPath, String rawValue) {
        ConfigField field = resolve(dottedPath);
        ConfigFileEditor.FieldValue before = ConfigFileEditor.get(configFile, field);
        ConfigFileEditor.set(configFile, field, rawValue);
        ConfigFileEditor.FieldValue after = ConfigFileEditor.get(configFile, field);
        return field.dottedPath() + ": " + before.value() + " -> " + after.value();
    }

    /** Case-insensitive prefix match over editable key names, for tab-completion. */
    public static List<String> suggestKeys(String partial) {
        String lower = partial == null ? "" : partial.toLowerCase(Locale.ROOT);
        List<String> out = new ArrayList<>();
        for (ConfigField field : ConfigFieldRegistry.all()) {
            if (field.dottedPath().toLowerCase(Locale.ROOT).startsWith(lower)) {
                out.add(field.dottedPath());
            }
        }
        return out;
    }

    /** Allowed values to suggest for a field's value argument (ENUM/BOOLEAN/named colors); empty
     *  for free-form fields (STRING/INT/LONG) or an unknown key. */
    public static List<String> suggestValues(String dottedPath) {
        ConfigField field = ConfigFieldRegistry.find(dottedPath);
        if (field == null) return List.of();
        return switch (field.type()) {
            case BOOLEAN -> List.of("true", "false");
            case ENUM, COLOR -> field.allowedValues() != null ? field.allowedValues() : ConfigField.NAMED_COLORS;
            case STRING, INT, LONG -> List.of();
        };
    }

    private static ConfigField resolve(String dottedPath) {
        if ("ai.apiKey".equalsIgnoreCase(dottedPath)) {
            throw new ConfigEditException("ai.apiKey can't be viewed or set via command — edit config.yml directly.");
        }
        if ("ai.models".equalsIgnoreCase(dottedPath)) {
            throw new ConfigEditException("ai.models is a list — use /aicompanion models list|add|remove instead.");
        }
        ConfigField field = ConfigFieldRegistry.find(dottedPath);
        if (field == null) {
            throw new ConfigEditException("Unknown config key: " + dottedPath + " — use /aicompanion config to list valid keys.");
        }
        return field;
    }

    private static String formatLine(ConfigField field, ConfigFileEditor.FieldValue value) {
        String suffix = value.active() ? "" : " (commented out in config.yml, inactive)";
        return field.dottedPath() + " = " + value.value() + suffix;
    }
}
