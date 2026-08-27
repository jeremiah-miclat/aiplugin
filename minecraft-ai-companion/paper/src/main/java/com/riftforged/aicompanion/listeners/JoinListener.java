package com.riftforged.aicompanion.listeners;

import com.riftforged.aicompanion.AskProcessor;
import org.bukkit.Bukkit;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.plugin.Plugin;

/** Greets every join, not just a player's first ever — plain AI calls aren't metered by this
 *  plugin, so there's no cost to justify tracking who's already been greeted. Ports handleJoin's
 *  call site from watcher.js. */
public final class JoinListener implements Listener {
    private final Plugin plugin;
    private final AskProcessor askProcessor;

    public JoinListener(Plugin plugin, AskProcessor askProcessor) {
        this.plugin = plugin;
        this.askProcessor = askProcessor;
    }

    @EventHandler
    public void onJoin(PlayerJoinEvent event) {
        String name = event.getPlayer().getName();
        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> askProcessor.handleJoin(name));
    }
}
