package com.riftforged.aicompanion.neoforge;

import com.riftforged.aicompanion.ChatFormat;
import com.riftforged.aicompanion.DiscordWebhook;
import com.riftforged.aicompanion.GameBridge;
import com.riftforged.aicompanion.ItemClassifier;
import net.minecraft.ChatFormatting;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.function.Supplier;
import java.util.logging.Logger;

/**
 * GameBridge implementation for NeoForge on 26.1 (Mojang mappings). Gives items by constructing
 * an ItemStack directly from the vanilla item registry rather than shelling out to the "/give"
 * command - same reasoning as the other platform modules.
 */
public final class NeoForgeGameBridge implements GameBridge {
    private final Supplier<MinecraftServer> serverSupplier;
    private final Logger logger;
    private final DiscordWebhook discordWebhook;
    private final String botName;

    public NeoForgeGameBridge(Supplier<MinecraftServer> serverSupplier, Logger logger, DiscordWebhook discordWebhook, String botName) {
        this.serverSupplier = serverSupplier;
        this.logger = logger;
        this.discordWebhook = discordWebhook;
        this.botName = (botName == null || botName.isBlank()) ? ChatFormat.DEFAULT_BOT_NAME : botName;
    }

    @Override
    public CompletableFuture<Void> giveItem(String playerName, String itemId, int quantity,
                                             int maxEquipmentQuantity, int maxQuantity) {
        int maxQty = ItemClassifier.isEquipmentItem(itemId) ? maxEquipmentQuantity : maxQuantity;
        int clamped = Math.max(1, Math.min(maxQty, quantity));

        CompletableFuture<Void> future = new CompletableFuture<>();
        MinecraftServer server = serverSupplier.get();
        server.execute(() -> {
            try {
                ServerPlayer player = server.getPlayerList().getPlayerByName(playerName);
                if (player == null) {
                    logger.warning("[ai-companion] give skipped, player offline: " + playerName);
                    return;
                }
                Optional<Item> item = BuiltInRegistries.ITEM.getOptional(Identifier.parse(itemId));
                if (item.isEmpty()) {
                    logger.warning("[ai-companion] give skipped, unknown item id: " + itemId);
                    return;
                }
                ItemStack stack = new ItemStack(item.get(), clamped);
                // Mirrors vanilla's own GiveCommand: insert into inventory, drop whatever doesn't fit.
                boolean absorbedAll = player.getInventory().add(stack);
                if (!absorbedAll && !stack.isEmpty()) {
                    player.drop(stack, false, false);
                }
                logger.info("[ai-companion] gave " + clamped + "x " + itemId + " to " + playerName);
            } finally {
                future.complete(null);
            }
        });
        return future;
    }

    @Override
    public void sendChat(String message, boolean broadcast) {
        String safe = com.riftforged.aicompanion.ai.AiClient.sanitize(message, ChatFormat.MC_CHAT_LIMIT);
        logger.info("[ai-companion] -> " + safe);
        discordWebhook.send(safe);
        Component component = formatted(safe);
        MinecraftServer server = serverSupplier.get();
        server.execute(() -> {
            if (broadcast) {
                server.getPlayerList().broadcastSystemMessage(component, false);
            } else {
                logger.info("[ai-companion] (console only) " + safe);
            }
        });
    }

    @Override
    public void sendPrivate(String playerName, String message) {
        String safe = com.riftforged.aicompanion.ai.AiClient.sanitize(message, ChatFormat.MC_CHAT_LIMIT);
        Component component = formatted(safe);
        MinecraftServer server = serverSupplier.get();
        server.execute(() -> {
            ServerPlayer player = server.getPlayerList().getPlayerByName(playerName);
            if (player != null) player.sendSystemMessage(component);
        });
    }

    @Override
    public String rateLimitKey(String playerName) {
        MinecraftServer server = serverSupplier.get();
        ServerPlayer player = server.getPlayerList().getPlayerByName(playerName);
        if (player != null) {
            SocketAddress addr = player.connection.getRemoteAddress();
            if (addr instanceof InetSocketAddress inet && inet.getAddress() != null) {
                return inet.getAddress().getHostAddress();
            }
        }
        return "name:" + playerName.toLowerCase();
    }

    private Component formatted(String safe) {
        return Component.literal(botName).withStyle(s -> s.withColor(ChatFormatting.BLUE).withBold(true))
            .append(Component.literal(" > ").withStyle(s -> s.withColor(ChatFormatting.GRAY)))
            .append(Component.literal(safe).withStyle(s -> s.withColor(ChatFormatting.WHITE)));
    }
}
