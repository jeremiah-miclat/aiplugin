package com.riftforged.aicompanion.ai;

/**
 * Which AI backend to talk to — config's "ai.provider", defaulting to OPENROUTER so existing
 * setups (and the shipped default config) need zero changes.
 */
public enum AiProvider {
    /** OpenRouter's own endpoint — one key, access to models from virtually every provider. */
    OPENROUTER,
    /** Any endpoint implementing the same OpenAI-style chat/completions wire format: OpenAI
     *  itself, Groq, Together, Fireworks, DeepSeek, Mistral, xAI, a self-hosted Ollama/LM
     *  Studio/vLLM server, etc. Requires ai.baseUrl to be set. */
    OPENAI_COMPATIBLE,
    /** Anthropic's own Claude API — different wire format (x-api-key header, "max_tokens"
     *  required, response shape is content blocks rather than choices). */
    ANTHROPIC;

    public static AiProvider fromConfig(String value) {
        if (value == null) return OPENROUTER;
        return switch (value.trim().toLowerCase()) {
            case "openai-compatible", "openai_compatible", "openaicompatible", "openai" -> OPENAI_COMPATIBLE;
            case "anthropic", "claude" -> ANTHROPIC;
            default -> OPENROUTER;
        };
    }
}
