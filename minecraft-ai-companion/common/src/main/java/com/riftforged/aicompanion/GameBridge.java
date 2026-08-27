package com.riftforged.aicompanion;

import java.util.concurrent.CompletableFuture;

/**
 * The handful of platform-specific actions AskProcessor needs to actually affect the game — every
 * other part of AskProcessor (prompt construction, parsing, rate limiting, conversation memory) is
 * pure logic and doesn't need this. One implementation per Minecraft-version Fabric module (each
 * mapped differently), since this is the boundary where Minecraft's actual API gets called.
 */
public interface GameBridge {
    /**
     * Clamps quantity per the equipment/stackable rule (see ItemClassifier) and gives the item to
     * the named player, completing once it's actually happened (or failed) — callers may depend
     * on ordering relative to the reply that follows.
     */
    CompletableFuture<Void> giveItem(String playerName, String itemId, int quantity,
                                      int maxEquipmentQuantity, int maxQuantity);

    /** message is already shaped as "PlayerName: text". broadcast=true means server-wide. */
    void sendChat(String message, boolean broadcast);

    /** Sends privately to one player only, regardless of the broadcastReplies setting — for
     *  bookkeeping notices (cooldown/queue-full) that aren't news to anyone else. No-op if the
     *  player has since logged off. */
    void sendPrivate(String playerName, String message);

    /** Rate-limit key for a player — their IP if resolvable while online, else a name-based
     *  fallback (mirrors watcher.js's playerIps lookup, done live here instead of from a log). */
    String rateLimitKey(String playerName);
}
