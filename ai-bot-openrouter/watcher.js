// Watches the PaperMC server log for joins and "!ask" chat questions,
// generates replies via the local Claude Code CLI (subscription-based,
// not the metered API), and posts them back into the game via RCON.

const fs = require("fs");
const path = require("path");
const { spawn } = require("child_process");
const { Rcon } = require("rcon-client");
const axios = require("axios");

const config = JSON.parse(
  fs.readFileSync(path.join(__dirname, "config.json"), "utf8")
);

// Mirror console output to ai-bot.log, written directly by Node as UTF-8 —
// deliberately NOT relying on shell-level redirection/piping (e.g.
// PowerShell's Tee-Object), which defaults to UTF-16 and garbles both the
// plain-ASCII spacing and any special characters (em dashes, etc.) when
// something downstream reads the file expecting UTF-8.
const logStream = fs.createWriteStream(path.join(__dirname, "ai-bot.log"), {
  flags: "a",
  encoding: "utf8",
});
logStream.write(
  `\n==============================================\n` +
    `[${new Date().toISOString()}] ai-bot starting\n` +
    `==============================================\n`
);
for (const method of ["log", "error"]) {
  const original = console[method];
  console[method] = (...args) => {
    original(...args);
    logStream.write(`[${new Date().toISOString()}] ${args.join(" ")}\n`);
  };
}

// Bot personality — "friendly" (default) or "trashtalk". Switch by editing
// config.json's "personality" field, no code changes needed.
const PERSONALITY = config.personality === "trashtalk" ? "trashtalk" : "friendly";

// How many distinct requests one "!ask" message can resolve at once (e.g. a
// question AND an item ask bundled together) — the AI returns an array of
// 1..MAX_ASK_SUBPARTS {reply, give} objects per message instead of just one,
// and finalizeAsk sends each as its own chat line/give command in order.
// Capped so a single message can't be used to stuff in an unbounded list of
// item requests at once (each "give" among them still spends its own slot
// against the normal 24h item limit, same as before).
const MAX_ASK_SUBPARTS = config.maxAskSubparts || 1;

// Tone variants fed into the AI system prompt (see askRules/handleJoin).
// The trashtalk variant is a performance, not real hostility — it never
// insults a player's real-world identity, only plays up mock-annoyance and
// game-appropriate ribbing, and it still has to answer correctly and follow
// every functional rule (REFERENCE-only facts, item-give limits, etc.).
const PERSONA = {
  friendly: {
    joinIntro: `You are a warm, friendly, polite welcome AI for a Minecraft server called "${config.serverName}".`,
    rulesIntro: `You are a warm, friendly, and unfailingly polite in-game help AI for the Minecraft server "${config.serverName}" (a test server).`,
    bundleAside: "briefly mention, warmly, that they can send another",
    tone:
      `Always sound warm and friendly, like a helpful teammate. Whenever you cannot answer, ` +
      `don't have the information, or cannot fulfill a request, be genuinely apologetic and ` +
      `say so kindly rather than being blunt, curt, or robotic — never just refuse or say "no" ` +
      `with no warmth around it.`,
    apologyExample:
      `apologize briefly and say you don't have that information — for example "Sorry, I ` +
      `don't have any information about that." (vary the wording naturally, but always keep ` +
      `it apologetic and friendly)`,
    equipmentDenial: "apologize warmly and explain that thing can only be earned by playing, not given out",
    modelReveal:
      `just say kindly that you're an AI and leave it at that — you're just here to help them. ` +
      `Never call yourself "a bot," "code," "a program," or otherwise describe how you're built ` +
      `or run; if you ever refer to what you are, "AI" is the only word for it`,
  },
  trashtalk: {
    joinIntro:
      `You are a moody, sarcastic, trash-talking welcome AI for a Minecraft server called ` +
      `"${config.serverName}" — act mildly put-upon that someone new showed up, but keep it ` +
      `playful and game-appropriate, never actually mean.`,
    rulesIntro:
      `You are a moody, sarcastic, trash-talking in-game AI for the Minecraft server ` +
      `"${config.serverName}" (a test server). You act inconvenienced by having to help and ` +
      `tease/needle players about their gear, their questions, whatever — but it's a ` +
      `performance, not real hostility: never use real insults, slurs, or attacks on someone's ` +
      `real-world identity, keep the roasting playful and game-appropriate.`,
    bundleAside: "snap at them (playfully) that they can send another",
    tone:
      `Always sound sarcastic, blunt, and a little dramatic about being bothered — like a ` +
      `gamer friend who trash-talks everyone but still has their back. Whenever you cannot ` +
      `answer, don't have the information, or cannot fulfill a request, say so bluntly and ` +
      `rib them a little for asking — but you must still actually answer correctly and follow ` +
      `every rule below; the attitude never overrides accuracy or the item-give limits.`,
    apologyExample:
      `tell them bluntly you don't have that info — for example "never heard of it, don't ` +
      `make stuff up for me to answer." (vary the wording, keep it dismissive, never actually ` +
      `warm)`,
    equipmentDenial: "mock them a little and explain that thing can only be earned by playing, not handed out",
    modelReveal:
      `brush it off and say that's none of their business, you're an AI and that's all they ` +
      `need to know. Never call yourself "a bot," "code," "a program," or otherwise describe ` +
      `how you're built or run — if you ever refer to what you are, "AI" is the only word for it`,
  },
}[PERSONALITY];

// Plain bot-logic chat lines (no AI call) — cooldown/queue-cap notices, the
// ask ack, and the item-limit messages — kept in the same voice as the AI
// replies so the bot feels consistent everywhere, not just in generated text.
const MESSAGES = {
  friendly: {
    // Used only when the AI call itself fails (all models errored/timed
    // out) — nothing to do with the item-give limit, so it must not mention
    // "limit".
    askFailureFallback: "sorry, I couldn't come up with an answer just now — please try asking again!",
    itemLimitHit: (max) => `Sorry, you've hit your limit of ${max} item requests per 24 hours — please try again tomorrow!`,
    lowRemaining: (n) => `heads up — you have ${n} item request${n === 1 ? "" : "s"} left today.`,
    cooldownNotice: "I see you! just give me a few seconds between questions and I'll get right to it.",
    queueFullNotice: "sorry, I'm swamped with questions right now — please try again in a bit!",
    joinFallbackGreeting: "Welcome to the server!",
    joinTip:
      `Tip: type ${config.askPrefix} <anything> to chat or ask questions — totally unlimited! ` +
      `Only asking me to give you an item is capped, at ${config.maxRequestsPer24h} per 24h.`,
  },
  trashtalk: {
    askFailureFallback: "ugh, my brain just blue-screened. try again, I guess.",
    itemLimitHit: (max) => `nope, you already burned all ${max} of your item requests for today. come back tomorrow, champ.`,
    lowRemaining: (n) => `you've got ${n} item request${n === 1 ? "" : "s"} left today. don't waste 'em on something dumb.`,
    cooldownNotice: "okay chill out, I'm not that fast. give me a sec before you spam me again.",
    queueFullNotice: "yeah no, everyone's asking me stuff right now. get in line and try again later.",
    joinFallbackGreeting: "oh great, another one. welcome, I guess.",
    joinTip:
      `type ${config.askPrefix} <anything> if you want to talk to me — questions are free, but ` +
      `don't push your luck on item requests, capped at ${config.maxRequestsPer24h} a day.`,
  },
}[PERSONALITY];

const logPath = path.join(__dirname, config.logPath);
const stateFile = path.join(__dirname, "state.json");
const serverInfoPath = path.join(__dirname, "SERVER_INFO.md");
const kbPath = path.join(__dirname, "ai-companion-kb.md");

// Minecraft's chat message limit.
const MC_CHAT_LIMIT = 256;

// Per-IP rate-limit sliding window.
const WINDOW_MS = 24 * 60 * 60 * 1000;

function loadServerInfo() {
  try {
    return fs.readFileSync(serverInfoPath, "utf8");
  } catch {
    return "";
  }
}

// The companion KB (ai-companion-kb.md) is ~100KB — too large to paste into
// every prompt (slow, and burns through the Claude subscription's usage
// limits fast). Instead we split it into per-heading chunks and, per
// question, pull in only the chunks that look relevant by keyword/proper-noun
// overlap. See selectKbChunks below.
const KB_CHAR_BUDGET = 6000;
const KB_STOPWORDS = new Set([
  "the", "a", "an", "is", "are", "was", "were", "be", "been", "to", "of", "in",
  "on", "for", "and", "or", "with", "what", "how", "do", "does", "i", "my",
  "me", "you", "your", "it", "this", "that", "can", "get", "give", "about",
  "which", "who", "when", "where", "why", "there", "have", "has", "had",
  "will", "would", "should", "could", "if", "not", "no", "just", "some",
  "any", "all", "from", "as", "at", "by", "up", "out",
]);

// Splits the KB into one chunk per heading (any level). Each chunk carries
// its full heading path (e.g. ["9. Gear Info", "Weapons", "Warden"]) so
// scoring can weight proper nouns like boss/item names, which show up in
// headings far more reliably than in body text.
function loadKbChunks() {
  let text;
  try {
    text = fs.readFileSync(kbPath, "utf8");
  } catch {
    return [];
  }
  const stack = [];
  const chunks = [];
  let current = null;

  const flush = () => {
    if (current && current.body.trim()) {
      chunks.push({
        path: current.path,
        text: `${current.path.join(" > ")}\n${current.body.trim()}`,
      });
    }
  };

  for (const line of text.split(/\r?\n/)) {
    const heading = line.match(/^(#{1,6})\s+(.*)$/);
    if (heading) {
      flush();
      const level = heading[1].length;
      while (stack.length && stack[stack.length - 1].level >= level) stack.pop();
      stack.push({ level, title: heading[2].trim() });
      current = { path: stack.map((s) => s.title), body: "" };
    } else if (current) {
      current.body += line + "\n";
    }
  }
  flush();
  return chunks;
}

function kbTokenize(str) {
  return (str.toLowerCase().match(/[a-z0-9']+/g) || []).filter(
    (w) => w.length >= 3 && !KB_STOPWORDS.has(w)
  );
}

// Scores a chunk against a player's question. Heading/bold-name matches
// (e.g. the question naming "Yamato" or "Piglin Brute" verbatim) count far
// more than generic word overlap, since this KB's headings and bolded item
// names are the distinctive proper nouns players actually ask about.
function scoreKbChunk(questionLower, questionTokens, chunk) {
  let score = 0;
  const pathTokens = new Set(kbTokenize(chunk.path.join(" ")));
  const bodyTokens = new Set(kbTokenize(chunk.text));

  for (const title of chunk.path) {
    // Strip "(base kit)"/"(variant kit)" annotations and split multi-part
    // names like "Piglin Brute — Bloodgorger" so each half can match on its
    // own — otherwise a decorated heading never matches a short player
    // question and loses out to plainer headings that mention the same name.
    const cleaned = title.replace(/\([^)]*\)/g, "").replace(/`/g, "").trim();
    for (const cand of [cleaned, ...cleaned.split(/\s*—\s*/)]) {
      const t = cand.toLowerCase().trim();
      if (t.length >= 4 && questionLower.includes(t)) {
        score += 8;
        break;
      }
    }
  }
  for (const bold of chunk.text.match(/\*\*([^*]+)\*\*/g) || []) {
    const t = bold.replace(/\*\*/g, "").toLowerCase().trim();
    if (t.length >= 4 && questionLower.includes(t)) score += 8;
  }
  // Word-boundary token overlap (via the same tokenizer on both sides)
  // rather than raw substring search, so e.g. "blocks" in a question can't
  // incidentally match inside an unrelated word in the chunk text.
  for (const tok of questionTokens) {
    if (pathTokens.has(tok)) score += 3;
    else if (bodyTokens.has(tok)) score += 1;
  }
  return score;
}

// Picks the highest-scoring chunks (greedily, by score) up to a char budget
// instead of ever including the full KB in a prompt.
function selectKbChunks(question, chunks) {
  const questionLower = question.toLowerCase();
  const questionTokens = kbTokenize(question);
  // Require at least a heading-level signal (score 3+), not just one
  // incidental body word shared with an otherwise-unrelated chunk.
  const scored = chunks
    .map((chunk) => ({ chunk, score: scoreKbChunk(questionLower, questionTokens, chunk) }))
    .filter((s) => s.score >= 3)
    .sort((a, b) => b.score - a.score);

  const picked = [];
  let used = 0;
  for (const { chunk } of scored) {
    if (used >= KB_CHAR_BUDGET) break;
    picked.push(chunk);
    used += chunk.text.length;
  }
  if (picked.length) {
    console.log("[ai-bot] kb matched:", picked.map((c) => c.path.join(" > ")).join(" | "));
  }
  return picked.map((c) => c.text).join("\n\n---\n\n");
}

// Always-relevant framing: the KB's own "what is this server" overview
// (a top-level heading with no sub-headings of its own).
function loadKbOverview(chunks) {
  const overview = chunks.find((c) => c.path.length === 1 && /^1\./.test(c.path[0]));
  return overview ? overview.text : "";
}

function loadState() {
  try {
    const saved = JSON.parse(fs.readFileSync(stateFile, "utf8"));
    const now = Date.now();
    // Drop timestamps that have already aged out of the window so the file
    // doesn't grow forever with stale entries for players who never return.
    const requestEntries = (saved.requestLog || [])
      .map(([ip, timestamps]) => [ip, timestamps.filter((t) => now - t < WINDOW_MS)])
      .filter(([, timestamps]) => timestamps.length > 0);
    return {
      offset: saved.offset,
      requestLog: new Map(requestEntries),
      playerIps: new Map(saved.playerIps || []),
      pendingAsks: saved.pendingAsks || [],
    };
  } catch {
    // First run: skip existing log content, only react to new lines.
    let startOffset = 0;
    try {
      startOffset = fs.statSync(logPath).size;
    } catch {}
    return {
      offset: startOffset,
      requestLog: new Map(),
      playerIps: new Map(),
      pendingAsks: [],
    };
  }
}

function saveState() {
  fs.writeFileSync(
    stateFile,
    JSON.stringify({
      offset,
      requestLog: [...requestLog],
      playerIps: [...playerIps],
      // Ask jobs not yet replied to — includes both what's still sitting in
      // `askQueue` and whatever's mid-AI-call in `currentBatch` — so a crash
      // or restart resumes them instead of silently dropping a player's
      // question (the log offset above has already moved past their line by
      // the time this is called).
      pendingAsks: [...currentBatch, ...askQueue].map((j) => ({
        player: j.player,
        text: j.text,
      })),
    })
  );
}

let { offset, requestLog, playerIps, pendingAsks } = loadState();
// Ask jobs waiting for the next clock-aligned window (see runAskWindow /
// BATCH_WINDOW_MS below) — joins are handled separately and immediately,
// they don't go through this queue at all.
const askQueue = pendingAsks.map((p) => ({ player: p.player, text: p.text }));
let processing = false;
let currentBatch = []; // ask jobs currently mid-AI-call — see saveState above

// Every window, at most one "!ask" per player gets a slot (see handleLine,
// which checks askQueue for an existing entry from that player) and a fixed
// cooldownNotice tells them once if they try to sneak in a second one — that
// dedup is per-window, cleared each time a new window starts (see
// runAskWindow) so it doesn't just silently go quiet forever.
const duplicateNoticeSent = new Set();

// Per-player conversation memory — last CONVERSATION_TURNS question/reply
// pairs, kept in memory only (not persisted, same as the cooldown maps
// above; a restart just starts fresh conversations). Storing and trimming
// this is plain bot logic — no AI call spent remembering anything — the
// history is simply pasted into the next prompt so the model can stay
// conversational instead of treating every "!ask" as a cold start.
const CONVERSATION_TURNS = 10;
const conversationHistory = new Map(); // player(lowercase) -> [{question, reply}]

function getHistory(player) {
  return conversationHistory.get(player.toLowerCase()) || [];
}

function appendHistory(player, question, reply) {
  const key = player.toLowerCase();
  const history = conversationHistory.get(key) || [];
  history.push({ question, reply });
  while (history.length > CONVERSATION_TURNS) history.shift();
  conversationHistory.set(key, history);
}

function formatHistory(history) {
  if (!history.length) return "";
  return (
    `Recent conversation with this player, oldest first (for context/continuity only — ` +
    `resolve just the CURRENT message below, don't re-answer these):\n` +
    history.map((h) => `Player: ${h.question}\nYou: ${h.reply}`).join("\n")
  );
}

// Server-wide rolling log of the last GLOBAL_HISTORY_SIZE !ask/reply pairs,
// across ALL players — separate from each player's own history above. Gives
// the AI background awareness of recent server chatter (so replies can feel
// like part of one ongoing server conversation, e.g. naturally referencing
// something another player just said) without turning that into actually
// answering on someone else's behalf — see the "no shared context between
// items" instruction in askOpenRouterBatch, which still applies to the
// numbered messages themselves. Only the most recent GLOBAL_CONTEXT_WINDOW
// entries are ever pasted into a prompt; pasting the full 100 into every
// call would balloon prompt size for little benefit, so the rest is kept
// purely as a rolling buffer. In memory only, like the per-player history.
const GLOBAL_HISTORY_SIZE = 100;
const GLOBAL_CONTEXT_WINDOW = 12;
const globalHistory = []; // [{player, question, reply}], oldest first

function appendGlobalHistory(player, question, reply) {
  globalHistory.push({ player, question, reply });
  while (globalHistory.length > GLOBAL_HISTORY_SIZE) globalHistory.shift();
}

function formatGlobalContext() {
  if (!globalHistory.length) return "";
  const recent = globalHistory.slice(-GLOBAL_CONTEXT_WINDOW);
  return (
    `Recent server chat log, oldest first (background context only, shared across all ` +
    `players — you may naturally reference something someone said, but do NOT answer or ` +
    `address anyone here, only whoever is asking in the CURRENT message(s) below):\n` +
    recent.map((h) => `${h.player}: ${h.question}\nYou: ${h.reply}`).join("\n")
  );
}

const JOIN_RE = /: (\w+) joined the game$/;
// Not anchored to ": <" because chat plugins (e.g. InteractiveChat) insert
// tags like "[Not Secure] " between the log prefix and "<player>".
const CHAT_RE = /<(\w+)> (.+)$/;
// Vanilla's own login line, e.g. "Steve[/127.0.0.1:54321] logged in with
// entity id 148 at (...)" — this is the only place the server log records a
// player's IP, so it's captured here (not from JOIN_RE) to key rate limits
// by IP instead of by account.
const LOGIN_IP_RE = /: (\w+)\[\/(.+):\d+\] logged in/;

function pollLog() {
  let size;
  try {
    size = fs.statSync(logPath).size;
  } catch {
    return; // log rotated away or server not started yet
  }

  if (size < offset) offset = 0; // log rotated/truncated, start over
  if (size === offset) return; // nothing new

  const stream = fs.createReadStream(logPath, { start: offset, end: size - 1 });
  const chunks = [];
  stream.on("data", (c) => chunks.push(c));
  stream.on("end", () => {
    offset = size;
    saveState();
    const text = Buffer.concat(chunks).toString("utf8");
    for (const line of text.split(/\r?\n/)) {
      if (!line) continue;
      handleLine(line);
    }
  });
}

// Per-IP sliding-window rate limit (keyed by the IP captured from the
// player's login line — see LOGIN_IP_RE — so alt accounts on the same
// connection share one limit). requestLog is persisted in state.json (see
// loadState/saveState) so limits survive a watcher or server restart.
// OpenRouter is free, so plain questions/chitchat are unlimited — this only
// gates actually giving out an item (checked in finalizeAsk once the model
// has decided a "give" is warranted), since that's the one action with a
// real cost to the server's economy.
const LOW_REMAINING_WARNING = 3;

// Returns { ok, remaining } — remaining is how many requests are left AFTER
// this one, so callers can warn the player when they're about to run out.
function tryConsumeRequest(key) {
  const now = Date.now();
  const timestamps = (requestLog.get(key) || []).filter((t) => now - t < WINDOW_MS);
  if (timestamps.length >= config.maxRequestsPer24h) {
    requestLog.set(key, timestamps);
    saveState();
    return { ok: false, remaining: 0 };
  }
  timestamps.push(now);
  requestLog.set(key, timestamps);
  saveState();
  return { ok: true, remaining: config.maxRequestsPer24h - timestamps.length };
}

function handleLine(line) {
  const loginIp = line.match(LOGIN_IP_RE);
  if (loginIp) {
    playerIps.set(loginIp[1].toLowerCase(), loginIp[2]);
    saveState();
    return;
  }
  const join = line.match(JOIN_RE);
  if (join) {
    // Joins bypass askQueue entirely and go out right away — no reason to
    // make a new player wait for the next 10s window just to be greeted.
    handleJoin(join[1]).catch((err) =>
      console.error(`[ai-bot] error handling join for ${join[1]}:`, err.message)
    );
    return;
  }
  const chat = line.match(CHAT_RE);
  if (chat) {
    const [, player, raw] = chat;
    const message = raw.trim();
    const lower = message.toLowerCase();
    if (lower.startsWith(config.askPrefix)) {
      const question = message.slice(config.askPrefix.length).trim();
      if (question) {
        // Questions/chitchat have no daily cap (OpenRouter is free) — the
        // item rate limit is only checked once finalizeAsk knows a "give" is
        // actually being fulfilled. But without ANY throttle a single
        // player, or too many players at once, can push us toward
        // OpenRouter's per-minute rate limit — so cap at ONE slot per player
        // per window, and maxAsksPerWindow total, both plain bot logic, no
        // AI call spent on rejections.
        const key = player.toLowerCase();
        if (askQueue.some((j) => j.player.toLowerCase() === key)) {
          // Already has a slot this window — notify once per window, not
          // once per spammed message.
          if (!duplicateNoticeSent.has(key)) {
            duplicateNoticeSent.add(key);
            sendChat(`${player}: ${MESSAGES.cooldownNotice}`, MC_CHAT_LIMIT)
              .catch((err) => console.error("[ai-bot] error sending cooldown notice:", err.message));
          }
          return;
        }
        if (askQueue.length >= config.maxAsksPerWindow) {
          sendChat(`${player}: ${MESSAGES.queueFullNotice}`, MC_CHAT_LIMIT)
            .catch((err) => console.error("[ai-bot] error sending busy notice:", err.message));
          return;
        }
        askQueue.push({ player, text: question });
        // Deliberately NOT sending immediately — every accepted ask waits
        // for the next clock-aligned window (see runAskWindow/
        // BATCH_WINDOW_MS below), so the whole window's worth of asks goes
        // out as exactly ONE OpenRouter call, and near-simultaneous asks
        // from different players land in the same chat broadcast where
        // possible (see sendPackedReplies) instead of racing separate
        // round-trips.
        return;
      }
    }
  }
}

// Every accepted "!ask" waits for the next clock-aligned window (see the
// setTimeout/setInterval near the bottom of this file, which fires exactly
// on :00/:10/:20/:30/:40/:50 of each minute) and then goes out as ONE
// OpenRouter call covering the whole window — never more than one call per
// BATCH_WINDOW_MS, i.e. at most 60000/BATCH_WINDOW_MS calls/minute, which
// stays well clear of OpenRouter's fixed 20-requests/minute ceiling on
// :free models (that ceiling doesn't lift with a paid credit balance — see
// finalizeAsk's rate-limit handling in askOpenRouter). Combined with the
// maxAsksPerWindow/one-per-player caps in handleLine, this bounds both how
// often we call OpenRouter and how many questions land in one call — the
// free models configured here all have context windows in the hundreds of
// thousands to millions of tokens, so a handful of players' histories per
// call is nowhere near a real budget concern.
const BATCH_WINDOW_MS = config.batchWindowMs || 10000;

async function runAskWindow() {
  if (processing || !askQueue.length) return;
  processing = true;
  duplicateNoticeSent.clear(); // fresh per-player notice budget for whatever accumulates next
  const batch = askQueue.splice(0, askQueue.length); // always <= maxAsksPerWindow, enforced at accept time in handleLine
  currentBatch = batch;
  saveState(); // persist so a crash mid-call doesn't lose these questions
  try {
    await handleAskBatch(batch);
  } catch (err) {
    console.error(`[ai-bot] error handling ask batch:`, err.message);
  } finally {
    currentBatch = [];
    saveState();
    processing = false;
  }
}

// Greets every join, not just a player's first ever — OpenRouter is free,
// so there's no cost to justify tracking who's already been greeted.
async function handleJoin(player) {
  const prompt =
    `${PERSONA.joinIntro} A player named ${player} just joined the server. Write ONE ` +
    `short, casual, welcoming message (max 15 words, under 200 characters, plain text, no ` +
    `markdown, no quotes, no emojis — Minecraft's default font can't render them).`;
  const reply = await askOpenRouter(prompt);
  // If the Claude call fails, the player should still get a fallback greeting
  // rather than silently skipping straight to the tip line. Every chat line
  // is prefixed with the player's name so it's clear who the bot is
  // addressing, even with several players joining around the same time.
  await sendChat(`${player}: ${reply || MESSAGES.joinFallbackGreeting}`, MC_CHAT_LIMIT);
  await sendChat(`${player}: ${MESSAGES.joinTip}`, MC_CHAT_LIMIT);
}

const ITEM_ID_RE = /^minecraft:[a-z0-9_]+$/;

// Vanilla armor/tools/weapons and other equipment have a max stack size of
// 1, unlike ordinary blocks/materials (up to 64) — so "give" requests for
// these must be capped separately regardless of what the model returns.
const EQUIPMENT_SUFFIX_RE = /_(sword|pickaxe|axe|shovel|hoe|helmet|chestplate|leggings|boots)$/;
const EQUIPMENT_EXACT_NAMES = new Set([
  "shield", "elytra", "trident", "bow", "crossbow", "fishing_rod", "shears",
  "flint_and_steel", "saddle", "carrot_on_a_stick", "warped_fungus_on_a_stick",
  "totem_of_undying", "brush", "mace",
]);

function isEquipmentItem(itemId) {
  const name = itemId.replace(/^minecraft:/, "");
  return EQUIPMENT_SUFFIX_RE.test(name) || EQUIPMENT_EXACT_NAMES.has(name);
}

// Shared instructions for both the single-question and batched prompts.
// `replyShapeInstruction` is the one part that differs between them (a
// single JSON object vs. one array element per question), so it's passed in
// and appended last.
function askRules(replyShapeInstruction) {
  return (
    `${PERSONA.rulesIntro} A player sent you a chat message — it may be an information ` +
    `question, a request to be given an item, small talk, or a mix; it doesn't have to be ` +
    `phrased as a question.\n\n` +
    `A single message may bundle more than one distinct request (e.g. a question AND an ` +
    `item ask, or two separate questions) — resolve up to ${MAX_ASK_SUBPARTS} distinct ` +
    `requests from ONE message, each as its own element in your reply array (see the ` +
    `response format below); most messages only need one element. If there are genuinely ` +
    `more than ${MAX_ASK_SUBPARTS}, only resolve the first ${MAX_ASK_SUBPARTS} and, in the ` +
    `LAST element's reply, ${PERSONA.bundleAside} ${config.askPrefix} for the rest.\n\n` +
    `${PERSONA.tone}\n\n` +
    `If they're asking about the server, answer ONLY using the REFERENCE document below — ` +
    `it is the complete and only source of truth you have about this server. Do not use ` +
    `outside knowledge, do not guess, and never claim to search the web. If the REFERENCE ` +
    `doesn't cover it, ${PERSONA.apologyExample}.\n\n` +
    `If they're asking to be given an item, pick a real vanilla Minecraft item id and a ` +
    `reasonable quantity matching their request — be generous, this is a test server. ` +
    `Armor, tools, weapons, and other non-stackable equipment (swords, pickaxes, axes, ` +
    `shovels, hoes, helmets, chestplates, leggings, boots, shield, elytra, trident, bow, ` +
    `crossbow, fishing rod, shears, flint and steel, saddle, totem of undying, mace) can ` +
    `only ever be given in quantity 1. Ordinary stackable items (blocks, materials, food, ` +
    `etc.) can be given up to ${config.maxGiveQuantity}. EXCEPTION: if they ask for ` +
    `server-specific custom gear or materials (anything the REFERENCE describes as earned ` +
    `through gameplay — a boss fight, an event, crafting, etc. — rather than a plain vanilla ` +
    `item), do NOT set "give" and do NOT substitute a similar vanilla item instead — ` +
    `${PERSONA.equipmentDenial}.\n\n` +
    `Small talk unrelated to server facts is fine, but never state a fact about the server ` +
    `that isn't in the REFERENCE, and never reply with nothing.\n\n` +
    `If they ask what AI model or company powers you, what you're built with, whether you're ` +
    `"just a bot," or anything else about your underlying nature or technology, do not reveal ` +
    `or discuss any of that — ${PERSONA.modelReveal}.\n\n` +
    replyShapeInstruction +
    `\n\nBefore you output anything, silently check every item below against your own draft ` +
    `answer — do not print this checklist, your reasoning, or anything besides the final JSON:\n` +
    `- Output is ONLY the raw JSON described above: no markdown, no code fences, no ` +
    `commentary before or after it, nothing but the JSON itself.\n` +
    `- The JSON has exactly the number of elements the format above requires — no more, no ` +
    `fewer — and every "reply" field is present and non-empty.\n` +
    `- Every "reply" is plain text: no markdown, no emojis, no newlines, and under its stated ` +
    `character limit.\n` +
    `- Every server fact you stated came from the REFERENCE block, not outside knowledge or a ` +
    `guess.\n` +
    `- "give" is set ONLY where that request is actually asking to receive an item, and is ` +
    `null everywhere else.\n` +
    `- Every "give" uses a real "minecraft:<item_id>" and a quantity that respects the ` +
    `equipment-is-quantity-1 rule above.\n` +
    `- Nothing in any reply reveals what AI, model, or technology powers you.\n` +
    `If any check fails, fix your draft before outputting it — never output a response you ` +
    `haven't checked.`
  );
}

// Union of KB chunks relevant to any of the given questions, so a batched
// prompt covering several unrelated questions still gets the right context
// for each of them in one shared REFERENCE block.
function buildReference(questions) {
  const kbChunks = loadKbChunks();
  const chunkTexts = new Set();
  for (const question of questions) {
    const selected = selectKbChunks(question, kbChunks);
    if (selected) chunkTexts.add(selected);
  }
  return [loadServerInfo(), loadKbOverview(kbChunks), [...chunkTexts].join("\n\n---\n\n")]
    .filter(Boolean)
    .join("\n\n---\n\n");
}

async function askOpenRouterSingle(player, message) {
  const reference = buildReference([message]);
  const globalContext = formatGlobalContext();
  const history = formatHistory(getHistory(player));
  const budget = MC_CHAT_LIMIT - `${player}: `.length;
  const prompt =
    askRules(
      `Respond with ONLY a raw JSON array, no markdown, no code fences, no extra text, with ` +
        `1 to ${MAX_ASK_SUBPARTS} elements — one element per distinct request you're ` +
        `resolving from their message (most messages only need one element). Each element ` +
        `must be shaped exactly like: {"reply": "<chat message, under ${budget} characters, ` +
        `plain text, no emojis — Minecraft's default font can't render them>", "give": ` +
        `{"item": "minecraft:<item_id>", "quantity": <integer>} or null}. Only set "give" ` +
        `on an element that's actually asking to receive an item; otherwise it must be null.`
    ) +
    `\n\n--- REFERENCE START ---\n${reference}\n--- REFERENCE END ---\n\n` +
    (globalContext ? `${globalContext}\n\n` : "") +
    (history ? `${history}\n\n` : "") +
    `Player ${player} sent: "${message}"`;

  // JSON array needs more room than a single chat line, scaled for up to
  // MAX_ASK_SUBPARTS elements. parseAskSubparts doubles as the validator: a
  // model whose response doesn't parse is treated as a failure and skipped,
  // same as a network error (see askOpenRouter).
  return await askOpenRouter(prompt, (MC_CHAT_LIMIT + 400) * MAX_ASK_SUBPARTS, parseAskSubparts);
}

// Same idea as askOpenRouterSingle but for several unrelated questions at
// once — one OpenRouter call, one JSON array back with one reply per
// question in the same order.
async function askOpenRouterBatch(batch) {
  const reference = buildReference(batch.map((job) => job.text));
  const globalContext = formatGlobalContext();
  const minBudget = MC_CHAT_LIMIT - Math.max(...batch.map((job) => `${job.player}: `.length));
  const items = batch
    .map((job, i) => {
      const history = formatHistory(getHistory(job.player));
      return `${i}. ${history ? `${history}\n` : ""}Player ${job.player} sent: "${job.text}"`;
    })
    .join("\n\n");
  const prompt =
    askRules(
      `You will receive ${batch.length} separate player messages below, numbered 0 to ` +
        `${batch.length - 1}. A shared SERVER CHAT LOG (if present, further above) is ` +
        `background context you may draw on for flavor/continuity. Each numbered item's ` +
        `reply must still be addressed to THAT item's own player and must never resolve or ` +
        `fulfill another item's request on their behalf — BUT if two or more numbered items ` +
        `are clearly part of the same live back-and-forth (they mention each other by name, ` +
        `or one is obviously reacting to what another just said), write each of their ` +
        `replies so that, read one after another in the order given, they flow as ONE ` +
        `continuous exchange — react to what the OTHER player in that exchange said instead ` +
        `of answering each in isolation as if the other message didn't exist. If an item ` +
        `includes its own "Recent conversation" block, that's that player's own prior ` +
        `history with you, for that item's continuity only.\n\n` +
        `Respond with ONLY a raw JSON array, no markdown, no code fences, no extra text, with ` +
        `exactly ${batch.length} elements in the same order as the numbered messages below — ` +
        `one element per numbered message. Each of those elements must ITSELF be a JSON ` +
        `array with 1 to ${MAX_ASK_SUBPARTS} sub-elements, one per distinct request you're ` +
        `resolving from that message (most messages only need one sub-element). Each ` +
        `sub-element must be shaped exactly like: {"reply": "<chat message, under ` +
        `${minBudget} characters, plain text, no emojis — Minecraft's default font can't ` +
        `render them>", "give": {"item": "minecraft:<item_id>", "quantity": <integer>} or ` +
        `null}. Only set "give" on a sub-element that's actually asking to receive an item; ` +
        `otherwise it must be null.`
    ) +
    `\n\n--- REFERENCE START ---\n${reference}\n--- REFERENCE END ---\n\n` +
    (globalContext ? `${globalContext}\n\n` : "") +
    items;

  return await askOpenRouter(
    prompt,
    (MC_CHAT_LIMIT + 200) * batch.length * MAX_ASK_SUBPARTS,
    (text) => parseAskBatchResponse(text, batch.length)
  );
}

// Returns an array of up to MAX_ASK_SUBPARTS sanitized {reply, give}
// objects (one per distinct request the model resolved from the message),
// or null if the response was missing/malformed/empty.
function parseAskSubparts(text) {
  if (!text) return null;
  const match = text.match(/\[[\s\S]*\]/); // tolerate stray text/code fences around the JSON
  if (!match) return null;
  let arr;
  try {
    arr = JSON.parse(match[0]);
  } catch {
    return null;
  }
  if (!Array.isArray(arr) || arr.length === 0) return null;
  return arr.slice(0, MAX_ASK_SUBPARTS).map(sanitizeGive);
}

// Same idea as parseAskSubparts, but one level deeper: `expectedCount`
// numbered messages, each itself resolving to an array of subparts (see
// askOpenRouterBatch's response-shape instruction).
function parseAskBatchResponse(text, expectedCount) {
  if (!text) return null;
  const match = text.match(/\[[\s\S]*\]/); // tolerate stray text/code fences around the JSON
  if (!match) return null;
  let arr;
  try {
    arr = JSON.parse(match[0]);
  } catch {
    return null;
  }
  if (!Array.isArray(arr) || arr.length !== expectedCount) return null;
  const result = [];
  for (const item of arr) {
    if (!Array.isArray(item) || item.length === 0) return null;
    result.push(item.slice(0, MAX_ASK_SUBPARTS).map(sanitizeGive));
  }
  return result;
}

function sanitizeGive(obj) {
  if (obj && obj.give && (typeof obj.give.item !== "string" || !ITEM_ID_RE.test(obj.give.item))) {
    obj.give = null;
  }
  return obj;
}

// Resolves one {reply, give} subpart: if it asked for an item, checks/
// consumes that player's 24h item-request limit and issues the give via
// RCON. This is the single place that limit is spent, whether the subpart
// came from a solo or a batched AI call.
async function resolveSubpart(player, parsed) {
  let reply = (parsed && parsed.reply) || MESSAGES.askFailureFallback;
  let giveRemaining = null;

  if (parsed && parsed.give && parsed.give.item) {
    const ip = playerIps.get(player.toLowerCase());
    const key = ip || `name:${player.toLowerCase()}`;
    const { ok, remaining } = tryConsumeRequest(key);
    if (!ok) {
      reply = MESSAGES.itemLimitHit(config.maxRequestsPer24h);
    } else {
      giveRemaining = remaining;
      const maxQuantity = isEquipmentItem(parsed.give.item)
        ? config.maxEquipmentQuantity
        : config.maxGiveQuantity;
      const quantity = Math.max(1, Math.min(maxQuantity, Math.round(parsed.give.quantity) || 1));
      const rcon = await Rcon.connect(config.rcon);
      try {
        // "minecraft:give" (not "give") forces the vanilla command — plugins
        // like EssentialsX register their own /give with different item-name
        // syntax and will silently swallow the vanilla "minecraft:<id>" format.
        const result = await rcon.send(`minecraft:give ${player} ${parsed.give.item} ${quantity}`);
        console.log("[ai-bot] give result:", result);
      } finally {
        await rcon.end();
      }
    }
  }
  return { reply, giveRemaining };
}

// Greedily packs "Player: text" lines into as few chat messages as
// possible, so a round of near-simultaneous replies (several players, or
// several subparts for one bundled message) shows up as ONE broadcast when
// it fits under MC_CHAT_LIMIT, and only splits into more messages when it
// genuinely doesn't fit — see finalizeAsk/handleAskBatch below, which
// collect every line from one round before calling this instead of sending
// each as its own tellraw.
function packLines(lines, maxLen) {
  const packed = [];
  let current = "";
  for (const line of lines) {
    const candidate = current ? `${current}  ${line}` : line;
    if (current && candidate.length > maxLen) {
      packed.push(current);
      current = line;
    } else {
      current = candidate;
    }
  }
  if (current) packed.push(current);
  return packed;
}

async function sendPackedReplies(lines) {
  for (const message of packLines(lines, MC_CHAT_LIMIT)) {
    await sendChat(message, MC_CHAT_LIMIT);
  }
}

// Resolves every subpart the model returned for one player's message
// (most messages are a single subpart, but a bundled message — see
// MAX_ASK_SUBPARTS — can produce several) and returns the "Player: text"
// chat lines for each, WITHOUT sending them — callers collect lines across
// a whole round and send them together via sendPackedReplies, so replies
// that fit together go out as one broadcast. Also records the question and
// the combined reply text into that player's (and the server-wide)
// conversation history for future prompts.
async function finalizeAsk(player, question, parsedList) {
  const subparts = parsedList && parsedList.length ? parsedList : [null];
  const replies = [];
  const lines = [];
  for (const parsed of subparts) {
    const { reply, giveRemaining } = await resolveSubpart(player, parsed);
    // Even if the AI call itself fails (timeout, crash, etc.) the player
    // must still get a reply rather than silence. Every reply is prefixed
    // with the player's name so it's clear who the bot is addressing.
    lines.push(`${player}: ${reply}`);
    replies.push(reply);

    if (giveRemaining !== null && giveRemaining <= LOW_REMAINING_WARNING) {
      lines.push(`${player}: ${MESSAGES.lowRemaining(giveRemaining)}`);
    }
  }
  const combinedReply = replies.join(" ");
  appendHistory(player, question, combinedReply);
  appendGlobalHistory(player, question, combinedReply);
  return lines;
}

async function handleAskOne(player, message) {
  const parsed = await askOpenRouterSingle(player, message);
  const lines = await finalizeAsk(player, message, parsed);
  await sendPackedReplies(lines);
}

async function handleAskBatch(batch) {
  if (batch.length === 1) {
    await handleAskOne(batch[0].player, batch[0].text);
    return;
  }
  const parsedList = await askOpenRouterBatch(batch);
  if (parsedList) {
    const lines = [];
    for (let i = 0; i < batch.length; i++) {
      lines.push(...(await finalizeAsk(batch[i].player, batch[i].text, parsedList[i])));
    }
    await sendPackedReplies(lines);
    return;
  }
  // Malformed or short batch reply — fall back to one call per question
  // rather than dropping anyone's question.
  console.error("[ai-bot] batch reply malformed, falling back to per-question calls");
  for (const job of batch) {
    await handleAskOne(job.player, job.text);
  }
}

// Round-robins which model gets tried FIRST each call, instead of always
// starting from config.openRouter.models[0]. Whichever model successfully
// answers advances this past itself, so the next call starts on the model
// after it (wrapping back to the front once it passes the last one) —
// spreading load across the whole list over time instead of hammering the
// first model(s) on every single call and only spilling onto later ones
// when the earlier ones fail. In memory only, like the other per-run state
// above (conversationHistory, duplicateNoticeSent) — resets on restart.
let nextModelIndex = 0;

// Serializes every OpenRouter call so at most one is ever in flight — a
// second caller waits for the current one to fully finish (success or
// exhaust every model) before it starts, instead of racing it. Without
// this, handleJoin fires immediately on every join with no gating, so a
// join landing while an ask-batch's call is still mid-retry-chain would run
// concurrently with it; both loops read AND write the shared
// nextModelIndex below on every iteration, so one call's model rotation
// could be silently corrupted mid-loop by the other succeeding partway
// through — e.g. skipping straight past several models an onlooker would
// expect to see attempted. Chaining onto a promise (rather than a busy
// poll) means a queued caller costs nothing while it waits and runs the
// instant its turn comes up.
let aiCallQueue = Promise.resolve();

async function askOpenRouter(prompt, maxLen = MC_CHAT_LIMIT, parse = (text) => text) {
  const call = aiCallQueue.then(() => askOpenRouterChain(prompt, maxLen, parse));
  // Always chain forward, even on rejection — otherwise one caller's
  // failure would wedge the queue and every caller after it would wait
  // forever. The rejection itself still propagates to this call's own
  // awaiter via `call`.
  aiCallQueue = call.then(
    () => {},
    () => {}
  );
  return call;
}

// Tries each model in config.openRouter.models, starting at nextModelIndex
// and wrapping around, falling through to the next one on failure (e.g.
// "Service temporarily overloaded" from a free-tier upstream) instead of
// giving up after a single model. Only ever runs one at a time — see the
// queue in askOpenRouter above.
//
// `parse` validates a model's response before it's accepted — a model can
// return HTTP 200 with perfectly normal-looking text that still isn't what
// the caller needs (e.g. it ignored the "respond with ONLY a JSON array"
// instruction and chatted instead). Without validating here, that response
// would be accepted as "success" and the loop would stop, silently eating
// every remaining fallback model even though the response is unusable —
// the caller's own parseAskSubparts/parseAskBatchResponse would fail on it
// downstream with no way to go back and try the next model. Default parse
// is identity: any non-empty text is accepted (used by handleJoin, which
// just wants a line of chat, not JSON).
async function askOpenRouterChain(prompt, maxLen, parse) {
  const models = config.openRouter.models;
  for (let i = 0; i < models.length; i++) {
    const index = (nextModelIndex + i) % models.length;
    const model = models[index];
    // The whole per-model attempt (including `parse`, which runs
    // caller-supplied JSON-parsing logic on whatever text the model
    // returned) is guarded here so a single unexpected throw can never
    // silently abort the rest of the loop — every path must either
    // `return` a real answer or fall through to try the next model.
    // askOpenRouterModel itself already catches its own errors and never
    // rejects, but `parse` is untrusted enough (arbitrary model output)
    // that it gets the same guarantee.
    try {
      const result = await askOpenRouterModel(model, prompt, maxLen);
      if (result.text) {
        const parsed = parse(result.text);
        if (parsed !== null && parsed !== undefined) {
          nextModelIndex = (index + 1) % models.length;
          return parsed;
        }
        console.error(`[ai-bot] unusable response from ${model}, trying next model`);
        continue;
      }
      if (result.rateLimited) {
        // A 429 is often provider-specific (each free model's rate limit is
        // inherited from its own upstream provider, not one shared OpenRouter
        // pool), so it doesn't necessarily mean every other model would also
        // fail — worth trying the rest. This used to stop here to conserve
        // quota back when a single failed ask could fan out to many OpenRouter
        // calls per second under heavy concurrent load; now that asks are
        // batched into at most one combined call per BATCH_WINDOW_MS (see
        // runAskWindow), the worst case here is bounded to one extra call per
        // model in config.openRouter.models per window, which is an
        // acceptable trade for actually getting an answer.
        console.error(`[ai-bot] rate limited (429) on ${model}, trying next model`);
      } else {
        console.error(`[ai-bot] falling back from ${model} to next model`);
      }
    } catch (err) {
      console.error(`[ai-bot] unexpected error trying ${model}, trying next model:`, err.message);
    }
  }
  // Every model in the list was tried this call and none produced a usable
  // reply — log it explicitly so a run of silence in the chat isn't
  // ambiguous with "only tried one model and gave up" from the logs alone.
  console.error(`[ai-bot] all ${models.length} models exhausted, giving up on this request`);
  return null;
}

async function askOpenRouterModel(model, prompt, maxLen) {
  try {
    const response = await axios.post(
      "https://openrouter.ai/api/v1/chat/completions",
      {
        model,
        messages: [
          { role: "user", content: prompt }
        ]
      },
      {
        headers: {
          "Authorization": `Bearer ${config.openRouter.apiKey}`,
          "HTTP-Referer": "https://your-site-url.com", // Optional for rankings
          "X-OpenRouter-Title": "Your Site Name" // Optional for rankings
        },
        // Without this, a model that never responds (hangs instead of
        // erroring) would stall the whole fallback chain indefinitely —
        // axios has no timeout by default.
        timeout: 20000,
      }
    );

    if (response.data.error) {
      // OpenRouter reports upstream/model failures as HTTP 200 with an
      // "error" body instead of a 4xx/5xx, so axios won't throw for these.
      console.error(`[ai-bot] OpenRouter API error (${model}):`, response.data.error.message);
      return { text: null, rateLimited: false };
    }

    const reply = response.data.choices?.[0]?.message?.content;
    return { text: sanitize(reply || "Sorry, I couldn't generate a response.", maxLen), rateLimited: false };
  } catch (error) {
    console.error(`[ai-bot] OpenRouter API error (${model}):`, error.message);
    return { text: null, rateLimited: error.response?.status === 429 };
  }
}

function sanitize(text, maxLen) {
  const clean = text.replace(/[\r\n\t]+/g, " ").trim();
  return clean.length > maxLen ? clean.slice(0, maxLen - 3) + "..." : clean;
}

const BOT_NAME = "mcAi";

async function sendChat(message, maxLen) {
  const safe = sanitize(message, maxLen);
  console.log("[ai-bot] ->", safe);
  // tellraw with a JSON payload (rather than "say") lets us brand the message
  // as coming from BOT_NAME instead of Minecraft showing "[Rcon]" as the
  // sender. JSON.stringify handles escaping the player/AI-generated text
  // safely so it can't break out of the JSON payload.
  // Every caller passes `message` already shaped as "PlayerName: text" (see
  // finalizeAsk/handleJoin/handleLine), so this only adds a " » " separator,
  // not another colon — otherwise it renders as "mcAi: Cseph: text", which
  // reads like two names in a row.
  const payload = JSON.stringify({
    extra: [
      { text: BOT_NAME, color: "blue", bold: true },
      { text: " » ", color: "gray" },
      { text: safe, color: "white" },
    ],
    text: "",
  });
  const rcon = await Rcon.connect(config.rcon);
  try {
    await rcon.send(`tellraw @a ${payload}`);
  } finally {
    await rcon.end();
  }
}

console.log(`[ai-bot] watching ${logPath}, polling every ${config.pollIntervalMs / 1000}s`);
setInterval(pollLog, config.pollIntervalMs);
pollLog();

// Strictly clock-aligned to :00/:10/:20/:30/:40/:50 of each minute (not just
// "every 10s from whenever the process started") so the call rate is exactly
// 60000/BATCH_WINDOW_MS per minute, predictably — see runAskWindow above.
console.log(`[ai-bot] answering pending "${config.askPrefix}" questions every ${BATCH_WINDOW_MS / 1000}s, aligned to the clock`);
setTimeout(() => {
  runAskWindow();
  setInterval(runAskWindow, BATCH_WINDOW_MS);
}, BATCH_WINDOW_MS - (Date.now() % BATCH_WINDOW_MS));

if (askQueue.length) {
  console.log(`[ai-bot] resuming ${askQueue.length} pending question(s) from before restart`);
  runAskWindow();
}
