package com.riftforged.aicompanion.fabric;

import com.riftforged.aicompanion.ChatFormat;
import com.riftforged.aicompanion.GameBridge;
import com.riftforged.aicompanion.ItemClassifier;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.registry.Registries;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.Identifier;

import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.function.Supplier;
import java.util.logging.Logger;

/**
 * GameBridge implementation for Fabric on 1.21.11 (Yarn mappings). Gives items by constructing an
 * ItemStack directly from the vanilla item registry rather than shelling out to the "/give"
 * command — Fabric doesn't have Bukkit's ecosystem of plugins overriding vanilla commands, so
 * there's no equivalent reason to force a specific command path the way Paper's ItemGiver does.
 */
public final class FabricGameBridge implements GameBridge {
    private final Supplier<MinecraftServer> serverSupplier;
    private final Logger logger;

    public FabricGameBridge(Supplier<MinecraftServer> serverSupplier, Logger logger) {
        this.serverSupplier = serverSupplier;
        this.logger = logger;
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
                ServerPlayerEntity player = server.getPlayerManager().getPlayer(playerName);
                if (player == null) {
                    logger.warning("[ai-companion] give skipped, player offline: " + playerName);
                    return;
                }
                Optional<Item> item = Registries.ITEM.getOptionalValue(Identifier.of(itemId));
                if (item.isEmpty()) {
                    logger.warning("[ai-companion] give skipped, unknown item id: " + itemId);
                    return;
                }
                ItemStack stack = new ItemStack(item.get(), clamped);
                player.giveItemStack(stack);
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
        Text component = formatted(safe);
        MinecraftServer server = serverSupplier.get();
        server.execute(() -> {
            if (broadcast) {
                server.getPlayerManager().broadcast(component, false);
            } else {
                logger.info("[ai-companion] (console only) " + safe);
            }
        });
    }

    @Override
    public void sendPrivate(String playerName, String message) {
        String safe = com.riftforged.aicompanion.ai.AiClient.sanitize(message, ChatFormat.MC_CHAT_LIMIT);
        Text component = formatted(safe);
        MinecraftServer server = serverSupplier.get();
        server.execute(() -> {
            ServerPlayerEntity player = server.getPlayerManager().getPlayer(playerName);
            if (player != null) player.sendMessage(component);
        });
    }

    @Override
    public String rateLimitKey(String playerName) {
        MinecraftServer server = serverSupplier.get();
        ServerPlayerEntity player = server.getPlayerManager().getPlayer(playerName);
        if (player != null) {
            SocketAddress addr = player.networkHandler.getConnectionAddress();
            if (addr instanceof InetSocketAddress inet && inet.getAddress() != null) {
                return inet.getAddress().getHostAddress();
            }
        }
        return "name:" + playerName.toLowerCase();
    }

    private static Text formatted(String safe) {
        return Text.literal(ChatFormat.BOT_NAME).styled(s -> s.withColor(Formatting.BLUE).withBold(true))
            .append(Text.literal(" > ").styled(s -> s.withColor(Formatting.GRAY)))
            .append(Text.literal(safe).styled(s -> s.withColor(Formatting.WHITE)));
    }
}
