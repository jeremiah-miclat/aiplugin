package com.riftforged.aicompanion.ai;

/** One "give this item" request the AI decided to fulfill. */
public record ItemGive(String item, int quantity) {
}
