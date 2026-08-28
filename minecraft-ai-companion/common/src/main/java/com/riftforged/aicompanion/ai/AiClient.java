package com.riftforged.aicompanion.ai;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.gson.JsonSyntaxException;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.function.Function;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Ports askOpenRouter/askOpenRouterChain/askOpenRouterModel from watcher.js: tries each
 * configured model in turn (round-robining which one starts first across calls), falling through
 * to the next on failure/rate-limit/unusable response, and serializes every call onto a single
 * background thread so at most one request is ever in flight — matching the original's
 * promise-chain queue, which exists so join greetings and batched asks can't race each other's
 * model-rotation state.
 *
 * Talks to whichever AiProvider is configured (default OPENROUTER, so existing setups need zero
 * changes). OPENROUTER and OPENAI_COMPATIBLE share the same OpenAI-style request/response shape —
 * only the endpoint (and a couple of OpenRouter-only headers) differ — so one code path covers
 * both; ANTHROPIC gets its own, since Claude's native API has a materially different wire format
 * (x-api-key auth, required "max_tokens", content-block responses instead of "choices").
 */
public final class AiClient {
    private static final String OPENROUTER_ENDPOINT = "https://openrouter.ai/api/v1/chat/completions";
    private static final String ANTHROPIC_ENDPOINT = "https://api.anthropic.com/v1/messages";
    private static final String ANTHROPIC_VERSION = "2023-06-01";

    private final Logger logger;
    private final AiProvider provider;
    private final String apiKey;
    private final String endpoint;
    private final List<String> models;
    private final HttpClient httpClient;
    private final ExecutorService serialExecutor;
    private volatile int nextModelIndex = 0;

    public AiClient(Logger logger, AiProvider provider, String apiKey, String baseUrl, List<String> models) {
        this.logger = logger;
        this.provider = provider;
        this.apiKey = apiKey;
        this.endpoint = switch (provider) {
            case OPENROUTER -> OPENROUTER_ENDPOINT;
            case ANTHROPIC -> ANTHROPIC_ENDPOINT;
            case OPENAI_COMPATIBLE -> {
                String trimmed = baseUrl == null ? "" : baseUrl.trim();
                if (trimmed.endsWith("/")) trimmed = trimmed.substring(0, trimmed.length() - 1);
                yield trimmed + "/chat/completions";
            }
        };
        this.models = models;
        this.httpClient = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(20))
            .build();
        this.serialExecutor = Executors.newSingleThreadExecutor(r -> {
            Thread t = new Thread(r, "ai-companion-ai-client");
            t.setDaemon(true);
            return t;
        });
    }

    public void shutdown() {
        serialExecutor.shutdownNow();
    }

    /** Identity parse: any non-empty text is accepted (used for the join greeting). */
    public CompletableFuture<String> ask(String prompt, int maxLen) {
        return ask(prompt, maxLen, text -> text);
    }

    public <T> CompletableFuture<T> ask(String prompt, int maxLen, Function<String, T> parse) {
        CompletableFuture<T> future = new CompletableFuture<>();
        serialExecutor.submit(() -> {
            try {
                future.complete(askChain(prompt, maxLen, parse));
            } catch (Throwable t) {
                future.completeExceptionally(t);
            }
        });
        return future;
    }

    private <T> T askChain(String prompt, int maxLen, Function<String, T> parse) {
        if (models.isEmpty()) {
            logger.severe("[ai-companion] no AI models configured");
            return null;
        }
        int start = nextModelIndex;
        logger.info("[ai-companion] starting model chain at index " + start + "/" + models.size()
            + " (" + models.get(start) + ")");
        for (int i = 0; i < models.size(); i++) {
            int index = (start + i) % models.size();
            String model = models.get(index);
            try {
                ModelResult result = askModel(model, prompt, maxLen);
                if (result.text() != null) {
                    T parsed;
                    try {
                        parsed = parse.apply(result.text());
                    } catch (RuntimeException e) {
                        logger.log(Level.WARNING, "[ai-companion] parse threw for " + model + ", trying next model", e);
                        continue;
                    }
                    if (parsed != null) {
                        logger.info("[ai-companion] answered using " + model
                            + " (index " + index + "/" + models.size() + "), next call starts at "
                            + ((index + 1) % models.size()));
                        nextModelIndex = (index + 1) % models.size();
                        return parsed;
                    }
                    logger.warning("[ai-companion] unusable response from " + model + ", trying next model");
                    continue;
                }
                if (result.rateLimited()) {
                    logger.warning("[ai-companion] rate limited (429) on " + model + ", trying next model");
                } else {
                    logger.warning("[ai-companion] falling back from " + model + " to next model");
                }
            } catch (RuntimeException e) {
                logger.log(Level.WARNING, "[ai-companion] unexpected error trying " + model + ", trying next model", e);
            }
        }
        logger.severe("[ai-companion] all " + models.size() + " models exhausted, giving up on this request");
        return null;
    }

    private record ModelResult(String text, boolean rateLimited) {
        static ModelResult failure(boolean rateLimited) {
            return new ModelResult(null, rateLimited);
        }
    }

    private ModelResult askModel(String model, String prompt, int maxLen) {
        return provider == AiProvider.ANTHROPIC
            ? askModelAnthropic(model, prompt, maxLen)
            : askModelOpenAiStyle(model, prompt, maxLen);
    }

    /** Shared by OPENROUTER and OPENAI_COMPATIBLE — both speak the same chat/completions format. */
    private ModelResult askModelOpenAiStyle(String model, String prompt, int maxLen) {
        JsonObject message = new JsonObject();
        message.addProperty("role", "user");
        message.addProperty("content", prompt);
        JsonArray messages = new JsonArray();
        messages.add(message);
        JsonObject body = new JsonObject();
        body.addProperty("model", model);
        body.add("messages", messages);
        if (provider == AiProvider.OPENAI_COMPATIBLE && !model.contains("compound")) {
            // Reasoning-capable models (Groq's included) can otherwise leak their chain-of-thought
            // into the actual reply content instead of just the final answer, even when the prompt
            // asks for output-only text — e.g. a reasoning model prefixing a reply with a
            // "**Reasoning**" section. "hidden" is Groq's own documented value for suppressing that
            // from the content field entirely. Scoped to OPENAI_COMPATIBLE only (not
            // OPENROUTER/ANTHROPIC, which have their own reasoning-control conventions) since an
            // unrecognized field risks a strict provider rejecting the whole request outright rather
            // than just ignoring it. Groq's compound/compound-mini are agentic systems, not reasoning
            // models, and reject this field with an HTTP 400 rather than ignoring it.
            body.addProperty("reasoning_format", "hidden");
        }

        HttpRequest.Builder requestBuilder = HttpRequest.newBuilder()
            .uri(URI.create(endpoint))
            .timeout(Duration.ofSeconds(20))
            .header("Content-Type", "application/json")
            .header("Authorization", "Bearer " + apiKey)
            .POST(HttpRequest.BodyPublishers.ofString(body.toString()));
        if (provider == AiProvider.OPENROUTER) {
            requestBuilder
                .header("HTTP-Referer", "https://your-site-url.com")
                .header("X-OpenRouter-Title", "Minecraft AI Companion");
        }

        HttpResponse<String> response;
        try {
            response = httpClient.send(requestBuilder.build(), HttpResponse.BodyHandlers.ofString());
        } catch (Exception e) {
            logger.log(Level.WARNING, "[ai-companion] AI API error (" + model + ")", e);
            return ModelResult.failure(false);
        }

        if (response.statusCode() == 429) {
            return ModelResult.failure(true);
        }
        if (response.statusCode() / 100 != 2) {
            logger.warning("[ai-companion] AI API error (" + model + "): HTTP " + response.statusCode()
                + " " + truncateForLog(response.body()));
            return ModelResult.failure(false);
        }

        JsonObject json;
        try {
            json = JsonParser.parseString(response.body()).getAsJsonObject();
        } catch (JsonSyntaxException | IllegalStateException e) {
            logger.warning("[ai-companion] AI API returned malformed JSON (" + model + ")");
            return ModelResult.failure(false);
        }

        // OpenRouter (and some OpenAI-compatible providers) report upstream/model failures as
        // HTTP 200 with an "error" body instead of a real non-2xx status.
        if (json.has("error")) {
            String msg = json.get("error").isJsonObject() && json.getAsJsonObject("error").has("message")
                ? json.getAsJsonObject("error").get("message").getAsString()
                : json.get("error").toString();
            logger.warning("[ai-companion] AI API error (" + model + "): " + msg);
            return ModelResult.failure(false);
        }

        String reply = null;
        try {
            JsonArray choices = json.getAsJsonArray("choices");
            if (choices != null && !choices.isEmpty()) {
                JsonObject choiceMessage = choices.get(0).getAsJsonObject().getAsJsonObject("message");
                if (choiceMessage != null && choiceMessage.has("content") && !choiceMessage.get("content").isJsonNull()) {
                    reply = choiceMessage.get("content").getAsString();
                }
            }
        } catch (RuntimeException ignored) {
            // fall through to the "couldn't generate" default below
        }
        if (reply == null || reply.isEmpty()) reply = "Sorry, I couldn't generate a response.";
        return new ModelResult(sanitize(reply, maxLen), false);
    }

    private ModelResult askModelAnthropic(String model, String prompt, int maxLen) {
        JsonObject message = new JsonObject();
        message.addProperty("role", "user");
        message.addProperty("content", prompt);
        JsonArray messages = new JsonArray();
        messages.add(message);
        JsonObject body = new JsonObject();
        body.addProperty("model", model);
        // Anthropic requires max_tokens (it's a hard ceiling, not a target — generous is free).
        // maxLen is a character budget; capped well under Claude's real output-token ceilings.
        body.addProperty("max_tokens", Math.min(8192, Math.max(256, maxLen)));
        body.add("messages", messages);

        HttpRequest request = HttpRequest.newBuilder()
            .uri(URI.create(endpoint))
            .timeout(Duration.ofSeconds(20))
            .header("Content-Type", "application/json")
            .header("x-api-key", apiKey)
            .header("anthropic-version", ANTHROPIC_VERSION)
            .POST(HttpRequest.BodyPublishers.ofString(body.toString()))
            .build();

        HttpResponse<String> response;
        try {
            response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
        } catch (Exception e) {
            logger.log(Level.WARNING, "[ai-companion] AI API error (" + model + ")", e);
            return ModelResult.failure(false);
        }

        if (response.statusCode() == 429) {
            return ModelResult.failure(true);
        }
        if (response.statusCode() / 100 != 2) {
            logger.warning("[ai-companion] AI API error (" + model + "): HTTP " + response.statusCode()
                + " " + truncateForLog(response.body()));
            return ModelResult.failure(false);
        }

        JsonObject json;
        try {
            json = JsonParser.parseString(response.body()).getAsJsonObject();
        } catch (JsonSyntaxException | IllegalStateException e) {
            logger.warning("[ai-companion] AI API returned malformed JSON (" + model + ")");
            return ModelResult.failure(false);
        }

        if (json.has("error")) {
            String msg = json.get("error").isJsonObject() && json.getAsJsonObject("error").has("message")
                ? json.getAsJsonObject("error").get("message").getAsString()
                : json.get("error").toString();
            logger.warning("[ai-companion] AI API error (" + model + "): " + msg);
            return ModelResult.failure(false);
        }

        String reply = null;
        try {
            JsonArray content = json.getAsJsonArray("content");
            if (content != null) {
                for (var block : content) {
                    JsonObject obj = block.getAsJsonObject();
                    if (obj.has("type") && "text".equals(obj.get("type").getAsString())
                        && obj.has("text") && !obj.get("text").isJsonNull()) {
                        reply = obj.get("text").getAsString();
                        break;
                    }
                }
            }
        } catch (RuntimeException ignored) {
            // fall through to the "couldn't generate" default below
        }
        if (reply == null || reply.isEmpty()) reply = "Sorry, I couldn't generate a response.";
        return new ModelResult(sanitize(reply, maxLen), false);
    }

    private static String truncateForLog(String body) {
        if (body == null) return "";
        return body.length() > 300 ? body.substring(0, 300) + "..." : body;
    }

    public static String sanitize(String text, int maxLen) {
        String clean = text.replaceAll("[\\r\\n\\t]+", " ").trim();
        return clean.length() > maxLen ? clean.substring(0, maxLen - 3) + "..." : clean;
    }
}
