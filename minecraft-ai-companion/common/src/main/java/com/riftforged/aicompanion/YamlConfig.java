package com.riftforged.aicompanion;

import org.yaml.snakeyaml.Yaml;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Thin wrapper over a snakeyaml-parsed config.yml, adding Bukkit-FileConfiguration-style dotted-
 * path lookups with defaults (e.g. "itemGiving.enabled") since snakeyaml itself only hands back a
 * plain nested Map. Used by the Fabric modules, which don't have Bukkit's own FileConfiguration —
 * Paper doesn't need this, it already gets equivalent behavior for free from Bukkit.
 */
public final class YamlConfig {
    private final Map<String, Object> root;

    @SuppressWarnings("unchecked")
    public YamlConfig(Path file) {
        Map<String, Object> loaded = null;
        if (Files.exists(file)) {
            try (InputStream in = Files.newInputStream(file)) {
                Object parsed = new Yaml().load(in);
                if (parsed instanceof Map) {
                    loaded = (Map<String, Object>) parsed;
                }
            } catch (IOException | RuntimeException e) {
                throw new IllegalStateException("Failed to parse " + file, e);
            }
        }
        this.root = loaded != null ? loaded : Map.of();
    }

    @SuppressWarnings("unchecked")
    private Object resolve(String dottedPath) {
        String[] parts = dottedPath.split("\\.");
        Object current = root;
        for (String part : parts) {
            if (!(current instanceof Map)) return null;
            current = ((Map<String, Object>) current).get(part);
            if (current == null) return null;
        }
        return current;
    }

    public String getString(String path, String def) {
        Object v = resolve(path);
        return v != null ? String.valueOf(v) : def;
    }

    public int getInt(String path, int def) {
        Object v = resolve(path);
        if (v instanceof Number n) return n.intValue();
        if (v != null) {
            try {
                return Integer.parseInt(String.valueOf(v).trim());
            } catch (NumberFormatException ignored) {
                // fall through to default
            }
        }
        return def;
    }

    public long getLong(String path, long def) {
        Object v = resolve(path);
        if (v instanceof Number n) return n.longValue();
        if (v != null) {
            try {
                return Long.parseLong(String.valueOf(v).trim());
            } catch (NumberFormatException ignored) {
                // fall through to default
            }
        }
        return def;
    }

    public boolean getBoolean(String path, boolean def) {
        Object v = resolve(path);
        if (v instanceof Boolean b) return b;
        if (v != null) return Boolean.parseBoolean(String.valueOf(v).trim());
        return def;
    }

    @SuppressWarnings("unchecked")
    public List<String> getStringList(String path) {
        Object v = resolve(path);
        if (v instanceof List<?> list) {
            List<String> out = new ArrayList<>();
            for (Object o : list) out.add(String.valueOf(o));
            return out;
        }
        return List.of();
    }
}
