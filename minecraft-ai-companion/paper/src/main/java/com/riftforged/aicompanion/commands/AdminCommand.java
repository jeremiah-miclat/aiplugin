package com.riftforged.aicompanion.commands;

import com.riftforged.aicompanion.AiCompanionPlugin;
import com.riftforged.aicompanion.config.AdminConfigCommands;
import com.riftforged.aicompanion.config.ConfigEditException;
import com.riftforged.aicompanion.config.ConfigFileEditor;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.logging.Level;

/**
 * "/aicompanion reload|config|get|set|models" — lets an admin view and edit config.yml entirely
 * from chat, so routine tuning (chat colors, personality, rate limits, the AI model list, etc.)
 * never requires hand-editing the YAML file. "ai.apiKey" is deliberately excluded from get/set (see
 * ConfigFieldRegistry) since it's a secret that shouldn't be echoed into chat/server logs.
 * Permission and command registration live in plugin.yml ("aicompanion.reload", default op).
 */
public final class AdminCommand implements CommandExecutor, TabCompleter {
    private static final List<String> SUBCOMMANDS = List.of("reload", "config", "get", "set", "models");
    private static final List<String> MODEL_SUBCOMMANDS = List.of("list", "add", "remove");

    private final AiCompanionPlugin plugin;

    public AdminCommand(AiCompanionPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (args.length == 0) {
            sender.sendMessage(usage(label));
            return true;
        }
        switch (args[0].toLowerCase(Locale.ROOT)) {
            case "reload" -> handleReload(sender);
            case "config" -> handleConfig(sender);
            case "get" -> handleGet(sender, args);
            case "set" -> handleSet(sender, args);
            case "models" -> handleModels(sender, args);
            default -> sender.sendMessage(usage(label));
        }
        return true;
    }

    private void handleReload(CommandSender sender) {
        try {
            plugin.reload();
            sender.sendMessage(Component.text(
                "[AiCompanion] Reloaded config.yml, server-info.md, and kb/.", NamedTextColor.AQUA));
        } catch (RuntimeException e) {
            plugin.getLogger().log(Level.SEVERE, "[ai-companion] reload failed", e);
            sender.sendMessage(Component.text(
                "[AiCompanion] Reload failed — check the server console for details.", NamedTextColor.RED));
        }
    }

    private void handleConfig(CommandSender sender) {
        for (String line : AdminConfigCommands.listLines(configFile())) {
            sender.sendMessage(Component.text("[AiCompanion] " + line, NamedTextColor.AQUA));
        }
    }

    private void handleGet(CommandSender sender, String[] args) {
        if (args.length != 2) {
            sender.sendMessage(Component.text("Usage: /aicompanion get <key>", NamedTextColor.RED));
            return;
        }
        try {
            String line = AdminConfigCommands.get(configFile(), args[1]);
            sender.sendMessage(Component.text("[AiCompanion] " + line, NamedTextColor.AQUA));
        } catch (ConfigEditException e) {
            sender.sendMessage(Component.text("[AiCompanion] " + e.getMessage(), NamedTextColor.RED));
        }
    }

    private void handleSet(CommandSender sender, String[] args) {
        if (args.length < 3) {
            sender.sendMessage(Component.text("Usage: /aicompanion set <key> <value>", NamedTextColor.RED));
            return;
        }
        String key = args[1];
        String value = String.join(" ", List.of(args).subList(2, args.length));
        try {
            String result = AdminConfigCommands.set(configFile(), key, value);
            plugin.reload();
            sender.sendMessage(Component.text("[AiCompanion] " + result, NamedTextColor.AQUA));
        } catch (ConfigEditException e) {
            sender.sendMessage(Component.text("[AiCompanion] " + e.getMessage(), NamedTextColor.RED));
        } catch (RuntimeException e) {
            plugin.getLogger().log(Level.SEVERE, "[ai-companion] reload after set failed", e);
            sender.sendMessage(Component.text(
                "[AiCompanion] Value saved, but reload failed — check the server console.", NamedTextColor.RED));
        }
    }

    private void handleModels(CommandSender sender, String[] args) {
        if (args.length < 2) {
            sender.sendMessage(Component.text("Usage: /aicompanion models list|add|remove <id>", NamedTextColor.RED));
            return;
        }
        switch (args[1].toLowerCase(Locale.ROOT)) {
            case "list" -> sender.sendMessage(Component.text(
                "[AiCompanion] ai.models: " + String.join(", ", ConfigFileEditor.getModels(configFile())),
                NamedTextColor.AQUA));
            case "add" -> {
                if (args.length != 3) {
                    sender.sendMessage(Component.text("Usage: /aicompanion models add <id>", NamedTextColor.RED));
                    return;
                }
                try {
                    ConfigFileEditor.addModel(configFile(), args[2]);
                    plugin.reload();
                    sender.sendMessage(Component.text("[AiCompanion] Added model: " + args[2], NamedTextColor.AQUA));
                } catch (ConfigEditException e) {
                    sender.sendMessage(Component.text("[AiCompanion] " + e.getMessage(), NamedTextColor.RED));
                }
            }
            case "remove" -> {
                if (args.length != 3) {
                    sender.sendMessage(Component.text("Usage: /aicompanion models remove <id>", NamedTextColor.RED));
                    return;
                }
                try {
                    ConfigFileEditor.removeModel(configFile(), args[2]);
                    plugin.reload();
                    sender.sendMessage(Component.text("[AiCompanion] Removed model: " + args[2], NamedTextColor.AQUA));
                } catch (ConfigEditException e) {
                    sender.sendMessage(Component.text("[AiCompanion] " + e.getMessage(), NamedTextColor.RED));
                }
            }
            default -> sender.sendMessage(Component.text("Usage: /aicompanion models list|add|remove <id>", NamedTextColor.RED));
        }
    }

    private Path configFile() {
        return plugin.getDataFolder().toPath().resolve("config.yml");
    }

    private static Component usage(String label) {
        return Component.text("Usage: /" + label + " reload|config|get <key>|set <key> <value>|models list|add|remove <id>",
            NamedTextColor.RED);
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        List<String> out = new ArrayList<>();
        if (args.length == 1) {
            addMatching(out, SUBCOMMANDS, args[0]);
        } else if (args.length == 2 && ("get".equalsIgnoreCase(args[0]) || "set".equalsIgnoreCase(args[0]))) {
            addMatching(out, AdminConfigCommands.suggestKeys(args[1]), args[1]);
        } else if (args.length == 3 && "set".equalsIgnoreCase(args[0])) {
            addMatching(out, AdminConfigCommands.suggestValues(args[1]), args[2]);
        } else if (args.length == 2 && "models".equalsIgnoreCase(args[0])) {
            addMatching(out, MODEL_SUBCOMMANDS, args[1]);
        } else if (args.length == 3 && "models".equalsIgnoreCase(args[0]) && "remove".equalsIgnoreCase(args[1])) {
            addMatching(out, ConfigFileEditor.getModels(configFile()), args[2]);
        }
        return out;
    }

    private static void addMatching(List<String> out, List<String> candidates, String prefix) {
        String lower = prefix.toLowerCase(Locale.ROOT);
        for (String candidate : candidates) {
            if (candidate.toLowerCase(Locale.ROOT).startsWith(lower)) {
                out.add(candidate);
            }
        }
    }
}
