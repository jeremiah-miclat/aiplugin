package com.riftforged.aicompanion;

import java.util.Set;
import java.util.regex.Pattern;

/** Ports isEquipmentItem from watcher.js/Paper's ItemGiver — pure string classification, no
 *  platform API needed, so it's shared as-is. */
public final class ItemClassifier {
    private static final Pattern EQUIPMENT_SUFFIX_RE =
        Pattern.compile("_(sword|pickaxe|axe|shovel|hoe|helmet|chestplate|leggings|boots)$");
    private static final Set<String> EQUIPMENT_EXACT_NAMES = Set.of(
        "shield", "elytra", "trident", "bow", "crossbow", "fishing_rod", "shears",
        "flint_and_steel", "saddle", "carrot_on_a_stick", "warped_fungus_on_a_stick",
        "totem_of_undying", "brush", "mace"
    );

    private ItemClassifier() {}

    public static boolean isEquipmentItem(String itemId) {
        String name = itemId.startsWith("minecraft:") ? itemId.substring("minecraft:".length()) : itemId;
        return EQUIPMENT_SUFFIX_RE.matcher(name).find() || EQUIPMENT_EXACT_NAMES.contains(name);
    }
}
