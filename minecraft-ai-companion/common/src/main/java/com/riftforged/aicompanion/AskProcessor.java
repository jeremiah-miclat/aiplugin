package com.riftforged.aicompanion;

import com.riftforged.aicompanion.ai.AiClient;
import com.riftforged.aicompanion.ai.AskParser;
import com.riftforged.aicompanion.ai.AskSubpart;
import com.riftforged.aicompanion.kb.KnowledgeBase;
import com.riftforged.aicompanion.state.AskQueue;
import com.riftforged.aicompanion.state.ConversationMemory;
import com.riftforged.aicompanion.state.RateLimiter;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Orchestrates one join greeting or one ask-window batch, end to end — ports handleJoin/
 * handleAskOne/handleAskBatch/resolveSubpart/finalizeAsk/askRules/askOpenRouterSingle/
 * askOpenRouterBatch from watcher.js. Shared by every Fabric MC-version module via GameBridge,
 * the one seam where actual Minecraft API calls happen (giving items, sending chat, resolving a
 * player's IP) — everything else here (prompt construction, parsing, rate limiting, conversation
 * memory) is pure logic. Paper carries its own separate copy bound directly to Bukkit (predates
 * this shared module; see common/pom.xml for why it wasn't retrofitted to use this one).
 */
public final class AskProcessor {
    private static final int LOW_REMAINING_WARNING = 3;

    private final Logger logger;
    private final YamlBotConfig config;
    private final Persona persona;
    private final Messages messages;
    private final KnowledgeBase kb;
    private final AiClient ai;
    private final ConversationMemory memory;
    private final RateLimiter rateLimiter;
    private final GameBridge bridge;

    public AskProcessor(Logger logger, YamlBotConfig config, Messages messages, KnowledgeBase kb,
                         AiClient ai, ConversationMemory memory, RateLimiter rateLimiter, GameBridge bridge) {
        this.logger = logger;
        this.config = config;
        this.persona = config.personality();
        this.messages = messages;
        this.kb = kb;
        this.ai = ai;
        this.memory = memory;
        this.rateLimiter = rateLimiter;
        this.bridge = bridge;
    }

    // ---- join ----

    public CompletableFuture<Void> handleJoin(String player) {
        String prompt = persona.joinIntro(config.serverName()) +
            " A player named " + player + " just joined the server. Write ONE short, casual, " +
            "welcoming message (max 15 words, under 200 characters, plain text, no markdown, " +
            "no quotes, no emojis — Minecraft's default font can't render them). " +
            "Output ONLY that message and nothing else — no reasoning, no explanation of your " +
            "choices, no \"Reasoning:\" section, no preamble or meta-commentary before or after " +
            "it. The very first character of your response must be the first character of the " +
            "greeting itself.";
        return ai.ask(prompt, ChatFormat.MC_CHAT_LIMIT)
            .thenCompose(reply -> {
                String greeting = (reply == null || reply.isEmpty()) ? messages.joinFallbackGreeting() : reply;
                bridge.sendChat(player + ": " + greeting, config.broadcastReplies());
                bridge.sendChat(player + ": " + messages.joinTip(), config.broadcastReplies());
                return CompletableFuture.<Void>completedFuture(null);
            })
            .exceptionally(e -> {
                logger.log(Level.WARNING, "[ai-companion] error handling join for " + player, e);
                return null;
            });
    }

    // ---- ask batch/one ----

    public CompletableFuture<Void> handleAskBatch(List<AskQueue.AskJob> batch) {
        if (batch.size() == 1) {
            return handleAskOne(batch.get(0).player(), batch.get(0).text());
        }
        return askAiBatch(batch).thenCompose(parsedList -> {
            if (parsedList != null) {
                List<String> allLines = new ArrayList<>();
                CompletableFuture<Void> seq = CompletableFuture.completedFuture(null);
                for (int i = 0; i < batch.size(); i++) {
                    AskQueue.AskJob job = batch.get(i);
                    List<AskSubpart> subparts = parsedList.get(i);
                    seq = seq.thenCompose(v -> finalizeAsk(job.player(), job.text(), subparts)
                        .thenAccept(allLines::addAll));
                }
                return seq.thenCompose(v -> {
                    sendPackedReplies(allLines);
                    return CompletableFuture.<Void>completedFuture(null);
                });
            }
            logger.warning("[ai-companion] batch reply malformed, falling back to per-question calls");
            CompletableFuture<Void> seq = CompletableFuture.completedFuture(null);
            for (AskQueue.AskJob job : batch) {
                seq = seq.thenCompose(v -> handleAskOne(job.player(), job.text()));
            }
            return seq;
        });
    }

    public CompletableFuture<Void> handleAskOne(String player, String message) {
        return askAiSingle(player, message)
            .thenCompose(parsed -> finalizeAsk(player, message, parsed))
            .thenAccept(this::sendPackedReplies)
            .exceptionally(e -> {
                logger.log(Level.WARNING, "[ai-companion] error handling ask for " + player, e);
                sendPackedReplies(List.of(player + ": " + messages.askFailureFallback()));
                return null;
            });
    }

    private void sendPackedReplies(List<String> lines) {
        for (String message : ChatFormat.packLines(lines, ChatFormat.MC_CHAT_LIMIT)) {
            bridge.sendChat(message, config.broadcastReplies());
        }
    }

    // ---- prompt construction ----

    private String askRules(String replyShapeInstruction) {
        return persona.rulesIntro(config.serverName()) +
            " A player sent you a chat message — it may be an information question, a request to " +
            "be given an item, small talk, or a mix; it doesn't have to be phrased as a question.\n\n" +
            "A single message may bundle more than one distinct request (e.g. a question AND an " +
            "item ask, or two separate questions) — resolve up to " + config.maxAskSubparts() +
            " distinct requests from ONE message, each as its own element in your reply array (see " +
            "the response format below); most messages only need one element. If there are " +
            "genuinely more than " + config.maxAskSubparts() + ", only resolve the first " +
            config.maxAskSubparts() + " and, in the LAST element's reply, " + persona.bundleAside() +
            " " + config.askPrefix() + " for the rest.\n\n" +
            persona.tone() + "\n\n" +
            "If they're asking something SPECIFIC TO THIS SERVER — its rules, custom features, " +
            "economy, claims, custom commands, custom items/mechanics a plugin adds, or any fact " +
            "about how this particular server is set up — answer ONLY using the REFERENCE document " +
            "below; it is the complete and only source of truth you have about this server's own " +
            "customizations. Do not use outside knowledge, do not guess, and never claim to search " +
            "the web for anything server-specific. If the REFERENCE doesn't cover a server-specific " +
            "question, " + persona.apologyExample() + ".\n\n" +
            "If instead they're asking a general Minecraft question that has nothing to do with " +
            "this server's own customizations — standard vanilla mechanics, crafting, building " +
            "tips, mob behavior, general survival strategy, and the like — answer it normally using " +
            "your own general Minecraft knowledge; that doesn't need the REFERENCE, and refusing to " +
            "help with it would be needlessly unhelpful. If a question touches both (a vanilla " +
            "mechanic the REFERENCE also documents a server-specific twist on), go with whatever " +
            "the REFERENCE says over general knowledge.\n\n" +
            itemGivingRules() + "\n\n" +
            "Small talk is fine, but never state a fact ABOUT THIS SERVER SPECIFICALLY that isn't " +
            "in the REFERENCE, and never reply with nothing.\n\n" +
            "If they ask what AI model or company powers you, what you're built with, whether " +
            "you're \"just a bot,\" or anything else about your underlying nature or technology, do " +
            "not reveal or discuss any of that — " + persona.modelReveal() + ".\n\n" +
            replyShapeInstruction +
            "\n\nBefore you output anything, silently check every item below against your own draft " +
            "answer — do not print this checklist, your reasoning, or anything besides the final " +
            "JSON:\n" +
            "- Output is ONLY the raw JSON described above: no markdown, no code fences, no " +
            "commentary before or after it, nothing but the JSON itself.\n" +
            "- The JSON has exactly the number of elements the format above requires — no more, no " +
            "fewer — and every \"reply\" field is present and non-empty.\n" +
            "- Every \"reply\" is plain text: no markdown, no emojis, no newlines, and under its " +
            "stated character limit.\n" +
            "- Every server fact you stated came from the REFERENCE block, not outside knowledge or " +
            "a guess.\n" +
            (config.itemGivingEnabled()
                ? "- \"give\" is set ONLY where that request is actually asking to receive an item, " +
                    "and is null everywhere else.\n" +
                    "- Every \"give\" uses a real \"minecraft:<item_id>\" and a quantity that respects " +
                    "the equipment-is-quantity-1 rule above.\n"
                : "- \"give\" is null on every single element, with no exceptions — item giving is " +
                    "off on this server.\n") +
            "- Nothing in any reply reveals what AI, model, or technology powers you.\n" +
            "If any check fails, fix your draft before outputting it — never output a response you " +
            "haven't checked.";
    }

    /** Branches the whole item-giving instruction on the master on/off switch, rather than just
     *  telling the model the numeric limits are 0 — an AI told "you can give 0 items" tends to
     *  either ignore that and give some anyway, or produce a confusing "here's 0 diamonds" reply;
     *  telling it the feature doesn't exist here at all is unambiguous. resolveSubpart() also
     *  enforces this at runtime regardless of what the model does, so this prompt text is a
     *  quality/UX measure, not the only thing standing between a disabled toggle and a real give. */
    private String itemGivingRules() {
        if (!config.itemGivingEnabled()) {
            return "Item giving is turned OFF on this server. Never set \"give\" on any element, " +
                "for any reason, even if a player asks for one — if they ask to be given " +
                "something, " + persona.equipmentDenial() + ", making clear item requests aren't " +
                "fulfilled here at all (not that they're out of items, or on cooldown — the " +
                "feature itself is off).";
        }
        return "If they're asking to be given an item, pick a real vanilla Minecraft item id and a " +
            "reasonable quantity matching their request — be generous, this is a test server. " +
            "Armor, tools, weapons, and other non-stackable equipment (swords, pickaxes, axes, " +
            "shovels, hoes, helmets, chestplates, leggings, boots, shield, elytra, trident, bow, " +
            "crossbow, fishing rod, shears, flint and steel, saddle, totem of undying, mace) can " +
            "only ever be given in quantity 1. Ordinary stackable items (blocks, materials, food, " +
            "etc.) can be given up to " + config.itemGivingMaxQuantity() + ". EXCEPTION: if they ask " +
            "for server-specific custom gear or materials (anything the REFERENCE describes as " +
            "earned through gameplay — a boss fight, an event, crafting, etc. — rather than a plain " +
            "vanilla item), do NOT set \"give\" and do NOT substitute a similar vanilla item instead " +
            "— " + persona.equipmentDenial() + ".";
    }

    private CompletableFuture<List<AskSubpart>> askAiSingle(String player, String message) {
        String reference = kb.buildReference(List.of(message));
        String globalContext = memory.formatGlobalContext();
        String history = ConversationMemory.formatHistory(memory.getHistory(player));
        int budget = ChatFormat.MC_CHAT_LIMIT - (player + ": ").length();

        String prompt = askRules(
            "Respond with ONLY a raw JSON array, no markdown, no code fences, no extra text, with " +
            "1 to " + config.maxAskSubparts() + " elements — one element per distinct request " +
            "you're resolving from their message (most messages only need one element). Each " +
            "element must be shaped exactly like: {\"reply\": \"<chat message, under " + budget +
            " characters, plain text, no emojis — Minecraft's default font can't render them>\", " +
            "\"give\": {\"item\": \"minecraft:<item_id>\", \"quantity\": <integer>} or null}. Only " +
            "set \"give\" on an element that's actually asking to receive an item; otherwise it " +
            "must be null."
        ) +
            "\n\n--- REFERENCE START ---\n" + reference + "\n--- REFERENCE END ---\n\n" +
            (globalContext.isEmpty() ? "" : globalContext + "\n\n") +
            (history.isEmpty() ? "" : history + "\n\n") +
            "Player " + player + " sent: \"" + message + "\"";

        int maxLen = (ChatFormat.MC_CHAT_LIMIT + 400) * config.maxAskSubparts();
        return ai.ask(prompt, maxLen, text -> AskParser.parseSubparts(text, config.maxAskSubparts()));
    }

    private CompletableFuture<List<List<AskSubpart>>> askAiBatch(List<AskQueue.AskJob> batch) {
        String reference = kb.buildReference(batch.stream().map(AskQueue.AskJob::text).toList());
        String globalContext = memory.formatGlobalContext();
        int minBudget = ChatFormat.MC_CHAT_LIMIT - batch.stream()
            .mapToInt(j -> (j.player() + ": ").length()).max().orElse(0);

        StringBuilder items = new StringBuilder();
        for (int i = 0; i < batch.size(); i++) {
            AskQueue.AskJob job = batch.get(i);
            String history = ConversationMemory.formatHistory(memory.getHistory(job.player()));
            if (i > 0) items.append("\n\n");
            items.append(i).append(". ")
                .append(history.isEmpty() ? "" : history + "\n")
                .append("Player ").append(job.player()).append(" sent: \"").append(job.text()).append("\"");
        }

        String prompt = askRules(
            "You will receive " + batch.size() + " separate player messages below, numbered 0 to " +
            (batch.size() - 1) + ". A shared SERVER CHAT LOG (if present, further above) is " +
            "background context you may draw on for flavor/continuity. Each numbered item's reply " +
            "must still be addressed to THAT item's own player and must never resolve or fulfill " +
            "another item's request on their behalf — BUT if two or more numbered items are " +
            "clearly part of the same live back-and-forth (they mention each other by name, or one " +
            "is obviously reacting to what another just said), write each of their replies so that, " +
            "read one after another in the order given, they flow as ONE continuous exchange — " +
            "react to what the OTHER player in that exchange said instead of answering each in " +
            "isolation as if the other message didn't exist. If an item includes its own \"Recent " +
            "conversation\" block, that's that player's own prior history with you, for that item's " +
            "continuity only.\n\n" +
            "Respond with ONLY a raw JSON array, no markdown, no code fences, no extra text, with " +
            "exactly " + batch.size() + " elements in the same order as the numbered messages below " +
            "— one element per numbered message. Each of those elements must ITSELF be a JSON array " +
            "with 1 to " + config.maxAskSubparts() + " sub-elements, one per distinct request you're " +
            "resolving from that message (most messages only need one sub-element). Each sub-element " +
            "must be shaped exactly like: {\"reply\": \"<chat message, under " + minBudget +
            " characters, plain text, no emojis — Minecraft's default font can't render them>\", " +
            "\"give\": {\"item\": \"minecraft:<item_id>\", \"quantity\": <integer>} or null}. Only " +
            "set \"give\" on a sub-element that's actually asking to receive an item; otherwise it " +
            "must be null."
        ) +
            "\n\n--- REFERENCE START ---\n" + reference + "\n--- REFERENCE END ---\n\n" +
            (globalContext.isEmpty() ? "" : globalContext + "\n\n") +
            items;

        int maxLen = (ChatFormat.MC_CHAT_LIMIT + 200) * batch.size() * config.maxAskSubparts();
        return ai.ask(prompt, maxLen, text -> AskParser.parseBatchResponse(text, batch.size(), config.maxAskSubparts()));
    }

    // ---- resolution ----

    private record ResolvedSubpart(String reply, Integer giveRemaining) {}

    private CompletableFuture<ResolvedSubpart> resolveSubpart(String player, AskSubpart parsed) {
        String reply = (parsed != null && parsed.reply() != null && !parsed.reply().isEmpty())
            ? parsed.reply() : messages.askFailureFallback();

        if (parsed == null || parsed.give() == null || parsed.give().item() == null) {
            return CompletableFuture.completedFuture(new ResolvedSubpart(reply, null));
        }

        // Runtime safety net: the prompt already tells the model never to set "give" while
        // disabled (see itemGivingRules()), but a model can ignore instructions — so this check
        // is what actually guarantees nothing is ever handed out while the toggle is off, and it
        // REPLACES the model's reply text rather than trusting it, in case that text promised an
        // item ("sure, here you go!") that's now not going to show up.
        if (!config.itemGivingEnabled()) {
            return CompletableFuture.completedFuture(new ResolvedSubpart(messages.itemGivingDisabled(), null));
        }

        String key = bridge.rateLimitKey(player);
        RateLimiter.Result limit = rateLimiter.tryConsume(key, config.itemGivingMaxPerDay());
        if (!limit.ok()) {
            return CompletableFuture.completedFuture(
                new ResolvedSubpart(messages.itemLimitHit(config.itemGivingMaxPerDay()), null));
        }

        return bridge.giveItem(player, parsed.give().item(), parsed.give().quantity(),
                config.itemGivingMaxEquipmentQuantity(), config.itemGivingMaxQuantity())
            .thenApply(v -> new ResolvedSubpart(reply, limit.remaining()));
    }

    private CompletableFuture<List<String>> finalizeAsk(String player, String question, List<AskSubpart> parsedList) {
        List<AskSubpart> subparts = (parsedList != null && !parsedList.isEmpty())
            ? parsedList : java.util.Collections.singletonList(null);

        List<String> lines = new ArrayList<>();
        List<String> replies = new ArrayList<>();
        CompletableFuture<Void> seq = CompletableFuture.completedFuture(null);
        for (AskSubpart parsed : subparts) {
            seq = seq.thenCompose(v -> resolveSubpart(player, parsed).thenAccept(resolved -> {
                lines.add(player + ": " + resolved.reply());
                replies.add(resolved.reply());
                if (resolved.giveRemaining() != null && resolved.giveRemaining() <= LOW_REMAINING_WARNING) {
                    lines.add(player + ": " + messages.lowRemaining(resolved.giveRemaining()));
                }
            }));
        }
        return seq.thenApply(v -> {
            String combinedReply = String.join(" ", replies);
            memory.appendHistory(player, question, combinedReply);
            memory.appendGlobalHistory(player, question, combinedReply);
            return lines;
        });
    }
}
