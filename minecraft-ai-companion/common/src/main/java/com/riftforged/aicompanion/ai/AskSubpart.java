package com.riftforged.aicompanion.ai;

/** One {reply, give} resolution the AI returned for a distinct request within a message. */
public record AskSubpart(String reply, ItemGive give) {
}
