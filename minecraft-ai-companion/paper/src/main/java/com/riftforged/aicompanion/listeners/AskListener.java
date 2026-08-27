package com.riftforged.aicompanion.listeners;

import com.riftforged.aicompanion.BotConfig;
import com.riftforged.aicompanion.ChatBroadcaster;
import com.riftforged.aicompanion.Messages;
import com.riftforged.aicompanion.state.AskQueue;
import io.papermc.paper.event.player.AsyncChatEvent;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.plugin.Plugin;

/**
 * Ports the CHAT_RE/askPrefix branch of handleLine from watcher.js: one slot per player per
 * batch window, capped total per window, deliberately NOT sent immediately — every accepted ask
 * waits for the next clock-aligned window so a window's worth of asks goes out as one AI call.
 *
 * An ACCEPTED message shows in chat normally, same as the original (which merely tailed the
 * server log after the message was already broadcast). A message that will NEVER be addressed
 * (DUPLICATE/COOLDOWN/FULL) is cancelled instead — no reason to let a spammed "!ai ..." that's
 * just getting dropped also spam the visible chat for everyone else.
 *
 * Runs at HIGH priority with ignoreCancelled=true: high enough that most mute/filter plugins
 * (which typically run at LOW/NORMAL) have already had their say, so this only reacts to messages
 * that were actually going to be broadcast — but still before MONITOR, since Bukkit's convention
 * is that MONITOR handlers only observe the final outcome and must not modify the event
 * themselves, and this one needs to be able to cancel.
 */
public final class AskListener implements Listener {
    private final Plugin plugin;
    private final BotConfig config;
    private final Messages messages;
    private final AskQueue askQueue;

    public AskListener(Plugin plugin, BotConfig config, Messages messages, AskQueue askQueue) {
        this.plugin = plugin;
        this.config = config;
        this.messages = messages;
        this.askQueue = askQueue;
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onChat(AsyncChatEvent event) {
        String message = PlainTextComponentSerializer.plainText().serialize(event.message()).trim();
        String prefix = config.askPrefix();
        if (!message.regionMatches(true, 0, prefix, 0, prefix.length())) return;
        String question = message.substring(prefix.length()).trim();
        if (question.isEmpty()) return;

        String player = event.getPlayer().getName();
        AskQueue.EnqueueResult result = askQueue.enqueue(player, question, config.maxAsksPerWindow(), config.askCooldownMs());
        switch (result) {
            case ACCEPTED -> { /* waits for the next batch window, shows in chat normally */ }
            case DUPLICATE, COOLDOWN -> {
                event.setCancelled(true);
                if (askQueue.shouldNotifyRejection(player)) {
                    ChatBroadcaster.sendPrivate(plugin, player, messages.cooldownNotice(config.askCooldownMs() / 1000));
                }
            }
            case FULL -> {
                event.setCancelled(true);
                if (askQueue.shouldNotifyRejection(player)) {
                    ChatBroadcaster.sendPrivate(plugin, player, messages.queueFullNotice());
                }
            }
        }
    }
}
