package com.riftforged.aicompanion.commands;

import com.riftforged.aicompanion.AiCompanionPlugin;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;

import java.util.List;
import java.util.logging.Level;

/**
 * "/aicompanion reload" — re-reads config.yml, server-info.md, and kb/ (server-info.md/kb are
 * already read live on every question with no reload needed; this is for config.yml, which is
 * cached in BotConfig at load time) without a full server restart. Permission and command
 * registration live in plugin.yml ("aicompanion.reload", default op).
 */
public final class ReloadCommand implements CommandExecutor, TabCompleter {
    private final AiCompanionPlugin plugin;

    public ReloadCommand(AiCompanionPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (args.length != 1 || !args[0].equalsIgnoreCase("reload")) {
            sender.sendMessage(Component.text("Usage: /" + label + " reload", NamedTextColor.RED));
            return true;
        }
        try {
            plugin.reload();
            sender.sendMessage(Component.text(
                "[AiCompanion] Reloaded config.yml, server-info.md, and kb/.", NamedTextColor.AQUA));
        } catch (RuntimeException e) {
            plugin.getLogger().log(Level.SEVERE, "[ai-companion] reload failed", e);
            sender.sendMessage(Component.text(
                "[AiCompanion] Reload failed — check the server console for details.", NamedTextColor.RED));
        }
        return true;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        return args.length == 1 ? List.of("reload") : List.of();
    }
}
