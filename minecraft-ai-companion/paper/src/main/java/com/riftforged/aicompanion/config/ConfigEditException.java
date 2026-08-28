package com.riftforged.aicompanion.config;

/**
 * Thrown by {@link ConfigFileEditor} on a validation failure (bad value for a field's type) or a
 * lookup failure (key line not found, or found more than once — schema drift). The message is
 * written to be shown directly to the admin in chat, no wrapping needed.
 */
public final class ConfigEditException extends RuntimeException {
    public ConfigEditException(String message) {
        super(message);
    }
}
