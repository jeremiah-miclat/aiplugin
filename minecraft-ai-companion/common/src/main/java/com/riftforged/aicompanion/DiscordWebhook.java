package com.riftforged.aicompanion;

import com.google.gson.JsonObject;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Relays bot chat lines to a Discord channel via an incoming webhook — the simple integration
 * path: no dependency on DiscordSRV or any other bridge plugin being installed, works with any
 * Discord server, admin just pastes a webhook URL (Discord channel settings -> Integrations ->
 * Webhooks -> New Webhook -> Copy URL) into config.yml. Disabled (a silent no-op) whenever that
 * URL is blank, which is the shipped default.
 *
 * Fire-and-forget: a relay failure (network error, bad URL, Discord rate limit) is logged and
 * dropped, never allowed to affect the in-game reply that triggered it — Discord is a mirror of
 * what already happened in-game, not a dependency of it.
 */
public final class DiscordWebhook {
    private final String webhookUrl;
    private final String username;
    private final HttpClient httpClient;
    private final Logger logger;

    public DiscordWebhook(String webhookUrl, String username, Logger logger) {
        this.webhookUrl = webhookUrl;
        this.username = username;
        this.logger = logger;
        this.httpClient = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(10))
            .build();
    }

    public boolean isEnabled() {
        return webhookUrl != null && !webhookUrl.isBlank();
    }

    /** message is plain text, already shaped as "PlayerName: text" — same string that went to
     *  in-game chat. Discord webhooks accept up to 2000 chars; our chat lines are already well
     *  under Minecraft's own 256-char limit, so no truncation is needed here. */
    public void send(String message) {
        if (!isEnabled()) return;

        JsonObject body = new JsonObject();
        body.addProperty("content", message);
        if (username != null && !username.isBlank()) {
            body.addProperty("username", username);
        }

        HttpRequest request = HttpRequest.newBuilder()
            .uri(URI.create(webhookUrl))
            .timeout(Duration.ofSeconds(10))
            .header("Content-Type", "application/json")
            .POST(HttpRequest.BodyPublishers.ofString(body.toString()))
            .build();

        httpClient.sendAsync(request, HttpResponse.BodyHandlers.discarding())
            .whenComplete((response, error) -> {
                if (error != null) {
                    logger.log(Level.WARNING, "[ai-companion] Discord webhook relay failed", error);
                } else if (response.statusCode() / 100 != 2) {
                    logger.warning("[ai-companion] Discord webhook relay failed: HTTP " + response.statusCode());
                }
            });
    }
}
