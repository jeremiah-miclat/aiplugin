package com.riftforged.aicompanion.config;

import com.riftforged.aicompanion.ChatFormat;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Edits config.yml in place, line-by-line on the raw text, so every comment and untouched value is
 * preserved byte-for-byte — unlike SnakeYAML's Yaml().dump() or Bukkit's FileConfiguration.save(),
 * which both rewrite the whole file from a parsed Map and drop every comment. Used by the
 * "/aicompanion get|set|config|models" commands on every platform.
 *
 * <p>Every editable leaf key name in the current schema (see {@link ConfigFieldRegistry}) is unique
 * across the whole file, so a field is located by an anchored regex on its leaf key name alone
 * (indentation captured, not assumed) rather than by full YAML-nesting awareness. {@link #locate}
 * asserts exactly one match, which would catch a future schema change that introduces a name
 * collision.
 */
public final class ConfigFileEditor {
    /** Current on-disk value of a field, and whether its line is active (true) or commented out. */
    public record FieldValue(String value, boolean active) {}

    private record LocatedLine(int index, String indent, String rawValue, boolean commented) {}

    private static final Pattern LIST_ITEM = Pattern.compile("^(\\s*)-\\s*(.+?)\\s*$");
    private static final Pattern MODELS_LINE = Pattern.compile("^(\\s*)models:\\s*$");

    private ConfigFileEditor() {}

    public static FieldValue get(Path file, ConfigField field) {
        List<String> lines = readLines(file);
        LocatedLine located = locate(lines, field.leafKey());
        return new FieldValue(stripQuotes(located.rawValue().trim()), !located.commented());
    }

    public static void set(Path file, ConfigField field, String rawValue) {
        String formatted = validateAndFormat(field, rawValue);
        List<String> lines = readLines(file);
        LocatedLine located = locate(lines, field.leafKey());
        lines.set(located.index(), located.indent() + field.leafKey() + ": " + formatted);
        writeLines(file, lines);
    }

    public static List<String> getModels(Path file) {
        List<String> lines = readLines(file);
        int idx = findModelsLine(lines);
        List<String> models = new ArrayList<>();
        for (int i = idx + 1; i < lines.size(); i++) {
            Matcher item = LIST_ITEM.matcher(lines.get(i));
            if (!item.matches()) break;
            models.add(stripQuotes(item.group(2).trim()));
        }
        return models;
    }

    public static void addModel(Path file, String modelId) {
        String id = modelId.trim();
        if (id.isEmpty()) {
            throw new ConfigEditException("Model id cannot be empty.");
        }
        List<String> lines = readLines(file);
        int modelsIdx = findModelsLine(lines);
        int lastItemIdx = modelsIdx;
        String itemIndent = null;
        for (int i = modelsIdx + 1; i < lines.size(); i++) {
            Matcher item = LIST_ITEM.matcher(lines.get(i));
            if (!item.matches()) break;
            if (stripQuotes(item.group(2).trim()).equalsIgnoreCase(id)) {
                throw new ConfigEditException("Model already configured: " + id);
            }
            itemIndent = item.group(1);
            lastItemIdx = i;
        }
        if (itemIndent == null) {
            Matcher modelsLine = MODELS_LINE.matcher(lines.get(modelsIdx));
            modelsLine.matches();
            itemIndent = modelsLine.group(1) + "  ";
        }
        lines.add(lastItemIdx + 1, itemIndent + "- \"" + id + "\"");
        writeLines(file, lines);
    }

    public static void removeModel(Path file, String modelId) {
        String id = modelId.trim();
        List<String> lines = readLines(file);
        int modelsIdx = findModelsLine(lines);
        for (int i = modelsIdx + 1; i < lines.size(); i++) {
            Matcher item = LIST_ITEM.matcher(lines.get(i));
            if (!item.matches()) break;
            if (stripQuotes(item.group(2).trim()).equalsIgnoreCase(id)) {
                lines.remove(i);
                writeLines(file, lines);
                return;
            }
        }
        throw new ConfigEditException("Model not configured: " + id);
    }

    // --- validation ---------------------------------------------------------------------------

    private static String validateAndFormat(ConfigField field, String rawValue) {
        String value = rawValue == null ? "" : rawValue.trim();
        return switch (field.type()) {
            case STRING -> "\"" + value.replace("\"", "\\\"") + "\"";
            case INT -> {
                try {
                    yield String.valueOf(Integer.parseInt(value));
                } catch (NumberFormatException e) {
                    throw new ConfigEditException(field.dottedPath() + " must be a whole number, got: " + rawValue);
                }
            }
            case LONG -> {
                try {
                    yield String.valueOf(Long.parseLong(value));
                } catch (NumberFormatException e) {
                    throw new ConfigEditException(field.dottedPath() + " must be a whole number, got: " + rawValue);
                }
            }
            case BOOLEAN -> {
                if (value.equalsIgnoreCase("true")) yield "true";
                if (value.equalsIgnoreCase("false")) yield "false";
                throw new ConfigEditException(field.dottedPath() + " must be true or false, got: " + rawValue);
            }
            case ENUM -> {
                for (String allowed : field.allowedValues()) {
                    if (allowed.equalsIgnoreCase(value)) yield "\"" + allowed + "\"";
                }
                throw new ConfigEditException(field.dottedPath() + " must be one of "
                    + String.join(", ", field.allowedValues()) + " — got: " + rawValue);
            }
            case COLOR -> {
                String lower = value.toLowerCase(Locale.ROOT);
                if (ChatFormat.isHexColor(value)) yield "\"" + value + "\"";
                for (String named : ConfigField.NAMED_COLORS) {
                    if (named.equals(lower)) yield "\"" + named + "\"";
                }
                throw new ConfigEditException(field.dottedPath() + " must be a Minecraft color name ("
                    + String.join(", ", ConfigField.NAMED_COLORS) + ") or a \"#RRGGBB\" hex code — got: " + rawValue);
            }
        };
    }

    // --- line lookup ---------------------------------------------------------------------------

    /**
     * Finds the single line defining leafKey, active or commented-out. Anchored at line-start
     * (optional whitespace, optional "#" + whitespace, then the key name + ":") so prose in
     * multi-line comments that merely mentions the key elsewhere in a sentence never matches.
     */
    private static LocatedLine locate(List<String> lines, String leafKey) {
        Pattern pattern = Pattern.compile("^(\\s*)(#\\s*)?" + Pattern.quote(leafKey) + ":\\s*(.*)$");
        LocatedLine found = null;
        for (int i = 0; i < lines.size(); i++) {
            Matcher m = pattern.matcher(lines.get(i));
            if (!m.matches()) continue;
            if (found != null) {
                throw new ConfigEditException(
                    "Found more than one '" + leafKey + "' line in config.yml — please edit it manually.");
            }
            found = new LocatedLine(i, m.group(1), m.group(3), m.group(2) != null);
        }
        if (found == null) {
            throw new ConfigEditException(
                "Could not find a '" + leafKey + "' line in config.yml — please edit it manually.");
        }
        return found;
    }

    private static int findModelsLine(List<String> lines) {
        for (int i = 0; i < lines.size(); i++) {
            if (MODELS_LINE.matcher(lines.get(i)).matches()) return i;
        }
        throw new ConfigEditException("Could not find the 'models:' line in config.yml — please edit it manually.");
    }

    private static String stripQuotes(String s) {
        if (s.length() >= 2 && s.startsWith("\"") && s.endsWith("\"")) {
            return s.substring(1, s.length() - 1);
        }
        return s;
    }

    private static List<String> readLines(Path file) {
        try {
            return new ArrayList<>(Files.readAllLines(file, StandardCharsets.UTF_8));
        } catch (IOException e) {
            throw new IllegalStateException("Failed to read " + file, e);
        }
    }

    private static void writeLines(Path file, List<String> lines) {
        try {
            Files.write(file, lines, StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new IllegalStateException("Failed to write " + file, e);
        }
    }
}
