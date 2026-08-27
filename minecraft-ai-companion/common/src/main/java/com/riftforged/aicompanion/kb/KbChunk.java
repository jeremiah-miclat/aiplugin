package com.riftforged.aicompanion.kb;

import java.util.List;

/** One heading-bounded slice of the knowledge base markdown, with its full heading path. */
public record KbChunk(List<String> path, String text) {
}
