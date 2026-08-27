package com.riftforged.aicompanion;

import org.bukkit.Bukkit;
import org.bukkit.plugin.Plugin;

import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.regex.Pattern;

/**
 * Ports isEquipmentItem/the give branch of resolveSubpart from watcher.js. Gives items via the
 * vanilla "minecraft:give" console command (same as the original's RCON call) rather than
 * building an ItemStack directly through the item registry — that keeps this immune to Bukkit's
 * ongoing Material/ItemType churn and matches the original's deliberate choice to force vanilla
 * give syntax so plugins like EssentialsX can't intercept it with a different item-name format.
 */
public final class ItemGiver {
    private static final Pattern EQUIPMENT_SUFFIX_RE =
        Pattern.compile("_(sword|pickaxe|axe|shovel|hoe|helmet|chestplate|leggings|boots)$");
    private static final Set<String> EQUIPMENT_EXACT_NAMES = Set.of(
        "shield", "elytra", "trident", "bow", "crossbow", "fishing_rod", "shears",
        "flint_and_steel", "saddle", "carrot_on_a_stick", "warped_fungus_on_a_stick",
        "totem_of_undying", "brush", "mace"
    );

    private ItemGiver() {}

    public static boolean isEquipmentItem(String itemId) {
        String name = itemId.startsWith("minecraft:") ? itemId.substring("minecraft:".length()) : itemId;
        return EQUIPMENT_SUFFIX_RE.matcher(name).find() || EQUIPMENT_EXACT_NAMES.contains(name);
    }

    /**
     * Clamps quantity per the equipment/stackable rules and dispatches the give on the main
     * thread, completing once the command has actually run (mirrors the original awaiting its
     * RCON round trip before moving on).
     */
    public static CompletableFuture<Void> give(Plugin plugin, String playerName, String itemId, int quantity,
                                                int maxEquipmentQuantity, int maxGiveQuantity) {
        int maxQuantity = isEquipmentItem(itemId) ? maxEquipmentQuantity : maxGiveQuantity;
        int clamped = Math.max(1, Math.min(maxQuantity, quantity));
        String command = "minecraft:give " + playerName + " " + itemId + " " + clamped;
        CompletableFuture<Void> future = new CompletableFuture<>();
        Bukkit.getScheduler().runTask(plugin, () -> {
            try {
                boolean ok = Bukkit.dispatchCommand(Bukkit.getConsoleSender(), command);
                plugin.getLogger().info("[ai-companion] give result (" + command + "): " + ok);
            } finally {
                future.complete(null);
            }
        });
        return future;
    }
}
