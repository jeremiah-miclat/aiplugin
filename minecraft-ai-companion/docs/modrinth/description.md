# Modrinth "Description" field

Paste everything below the `---` into Modrinth's Description field (it renders markdown). This
single blob now includes the full setup guide and the Groq API key guide inline — Modrinth only
takes one paste, so there's no separate "Full Guide" section to fill in.

---

**AI Companion** is an in-game AI chat helper for your Minecraft server. Players greet it or ask
it questions in chat, and it answers using *your own* server rules, lore, and info — not generic
made-up answers. It can optionally hand out items too, with limits you control.

Nothing about it is pre-loaded with any particular server's content — every default ships as an
empty-ish template meant to be replaced. You write what it knows; it just answers in your voice.

## ⚠️ Free-tier AI key required

This plugin/mod needs an API key from an AI provider to work (Groq by default — a real permanent
free tier, no credit card). **Only ever use a free-tier account with no payment method attached.**
The key lives in a plaintext config file on whatever machine hosts your server, and hosting
(especially cheap/shared hosting) can be compromised or have other people with filesystem access.
A leaked free-tier key costs you nothing worse than rate-limiting on a $0 account; a leaked paid
key can run up a real bill. See **the "Get a free Groq API key" section below** for the 2-minute
signup, and **"Content disclosures" right below this** for the full reasoning.

## Content disclosures

**This project connects to a third-party AI API.** To generate chat replies, this plugin/mod
sends the text of player questions (and short server context you write yourself — no personal
player data) to an AI provider's API. By default that provider is **Groq**; the config also
supports OpenAI-compatible endpoints, OpenRouter, and Anthropic if you choose to switch. You must
supply your own API key from that provider — none is bundled, and the plugin/mod does nothing
until you add one.

Every AI provider option documented for this project (Groq, OpenRouter's `:free` models, local
Ollama) has a genuine free tier that needs no card — see the "Get a free Groq API key" section
below for the default path, and don't attach billing to that account.

**Optional Discord relay.** If you turn on the optional Discord webhook relay, chat lines get
posted to a Discord channel via a webhook URL you provide. This is off by default and, like the AI
key, is a credential you supply and control — treat a webhook URL with the same care (don't share
it, rotate it if it leaks).

No player data leaves your server other than the chat text needed to generate a reply, sent
directly to the AI provider you configured.

## Features

- **Answers questions in chat** — type `!ai <question>` (prefix configurable), get a reply built
  from your `server-info.md` and `kb/` knowledge-base files.
- **Join greetings** — welcomes players when they log in.
- **Personality modes** — `friendly` or `trashtalk`, your choice.
- **Per-player and server-wide conversation memory**, so it remembers context across messages.
- **Optional item giving** — the AI can hand out items on request, capped by daily-per-player
  limits, stack-size limits, and separate equipment limits. Turn it off entirely with one config
  flag if you don't want it.
- **Batched AI calls** — questions from multiple players in the same short window go out as one
  AI request, not one per player, so it's cheap to run even on a busy server.
- **Model fallback/rotation** across your configured models, so a single overloaded/rate-limited
  model doesn't take the whole thing down.
- **Optional Discord relay** — mirror join greetings and `!ai` replies into a Discord channel via
  a plain incoming webhook. Off by default; no bridge plugin required.
- **`/aicompanion reload`** — apply config changes without restarting the server.
- **Crash-safe state persistence** — cooldowns and conversation memory survive restarts.

## Supported platforms

| Platform | Minecraft versions |
|---|---|
| **Paper** | one jar, forward-compatible from 1.21.11 through the 26.x line |
| **Fabric** | 1.21.11, 26.1, 26.2 |
| **Forge** | 1.21.11, 26.1, 26.2 |
| **NeoForge** | 1.21.11, 26.1, 26.2 |

Every platform uses the exact same `config.yml` shape and keys — copy one platform's config to
another and it just works.

## Quick start

1. Drop the jar in `plugins/` (Paper) or `mods/` (Fabric/Forge/NeoForge) and start the server once.
   That generates `config.yml`, `server-info.md`, and a `kb/` folder.
2. Get a **free** Groq API key: **full steps in the "Get a free Groq API key" section below**.
   Paste it into `config.yml` under `ai.apiKey`.
3. Fill in `server-info.md` (short bullet points: server name, rules, key commands) and drop your
   own `.md` files into `kb/` for anything else players might ask about.
4. Run `/aicompanion reload` (or restart) and you're live.

Full configuration reference, every option explained: **see "Full setup guide" below**.

## Full setup guide

Everything you need to install, configure, and run AI Companion on any of the four supported
platforms.

### 1. Install

| Platform | Where the jar goes | Files generated on first start |
|---|---|---|
| Paper | `plugins/` | `plugins/AiCompanion/config.yml`, `server-info.md`, `kb/` |
| Fabric | `mods/` | `config/aicompanion/config.yml`, `server-info.md`, `kb/` |
| Forge | `mods/` | `config/aicompanion/config.yml`, `server-info.md`, `kb/` |
| NeoForge | `mods/` | `config/aicompanion/config.yml`, `server-info.md`, `kb/` |

Pick the jar matching your loader **and** Minecraft version (1.21.11, 26.1, or 26.2 — Paper ships
one jar that covers all of them). Start the server once to generate the files above, then it won't
answer anything useful until you fill in two of them: an API key, and your own server info.

### 2. AI API key

**Use a free-tier account with no payment method attached — not a paid one.** This project's
config keeps `apiKey` as plain text in `config.yml` on your server's own filesystem; a free key
only risks hitting a rate limit if it ever leaks, a paid one risks a real bill. Every provider
option below has a genuine free tier that needs no card.

The shipped default is **Groq**, and it's the one this guide walks through step by step: see the
"Get a free Groq API key" section below.

Set these four fields under `ai:` in `config.yml`:

```yaml
ai:
  provider: "openai-compatible"   # shipped default — see the Groq key guide below
  apiKey: "..."                   # your own free-tier key — never share one across installs
  baseUrl: "https://api.groq.com/openai/v1"
  models:
    - "groq/compound"
    - "openai/gpt-oss-120b"
    - "openai/gpt-oss-20b"
```

(The config also supports OpenRouter and Anthropic as alternate providers if you'd rather use
those — both have free options too — but this guide only covers the Groq path, since it's the
default and needs no extra setup beyond the key itself.)

After changing this, run `/aicompanion reload` or restart the server. Pending questions, item-give
cooldowns, and conversation history all survive a reload — only `config.yml`/`server-info.md`/
`kb/` get re-read. Needs op-equivalent permission (Paper: `aicompanion.reload`, default op, alias
`/aic`; Fabric/Forge/NeoForge: vanilla permission level 4, same as `/op`).

### 3. Server info

Nothing ships pre-loaded with any particular server's content — every default is a template meant
to be replaced.

- **`server-info.md`** — short, factual bullet points: server name, rules, gamemode, key
  commands/plugins, links. This gets pasted into every prompt, so keep it tight.
- **`kb/`** — a folder, not a single file. Drop in as many `.md` files as you want for anything
  `server-info.md` doesn't have room for (game systems, lore, item lists, event rules). Ships with
  one example file showing the format — delete the example, write your own. Files are read in
  filename order (`01-...`, `02-...`) if order matters. Give things clear headings/**bold** text —
  that's what gets matched against a player's question.

Both are re-read live on every question — no reload needed for these two.

### 4. Item giving

```yaml
itemGiving:
  enabled: true            # master switch — false means the bot NEVER gives an item, no exceptions
  maxPerDay: 10             # per player, rolling 24h window
  maxQuantity: 64            # ordinary stackable items
  maxEquipmentQuantity: 1    # weapons/armor/tools/etc (vanilla max stack size 1 anyway)
```

Turning `enabled: false` doesn't just stop gives from happening — the AI is told never to offer
one, and if it ignores that, the plugin overrides its reply with a plain "item giving is off here"
message instead of letting a promised-but-undelivered item slip through.

### 5. Other tunables

| Key | What it does |
|---|---|
| `botName` | the name shown in chat before every bot message, default `"mcAi"` |
| `personality` | `"friendly"` or `"trashtalk"` — the bot's tone |
| `askPrefix` | the chat prefix players type, default `"!ai"` |
| `batchWindowMs` | how often queued questions go out as one AI call (default 10s) |
| `maxAsksPerWindow` | total questions accepted per window, across all players |
| `askCooldownSeconds` | per-player pacing independent of `batchWindowMs` |
| `maxAskSubparts` | how many distinct requests one message can resolve at once |
| `broadcastReplies` | `true` = bot replies go to server-wide chat; `false` = console log only |

### 6. Discord relay (optional)

Every bot chat line (join greetings, `!ai` replies) can be relayed into a Discord channel via an
incoming webhook — no bridge plugin needed:

1. In Discord: target channel → Settings → Integrations → Webhooks → New Webhook → Copy Webhook
   URL.
2. In `config.yml`:
   ```yaml
   discord:
     webhookUrl: "https://discord.com/api/webhooks/..."
     username: ""   # optional — overrides the name Discord shows; blank uses the webhook's own
   ```
3. `/aicompanion reload` or restart.

Leave `webhookUrl` blank (the default) to keep this off. A relay failure (bad URL, Discord down,
rate-limited) is logged and dropped; it never affects the in-game reply that triggered it.

### Security notes

- Use a **free-tier** API key and Discord webhook — see "AI API key" above for why.
- Never commit `config.yml` with a real key in it anywhere, and never hand out a jar/config with
  your key baked in — every install needs its own.
- If a key ever ends up somewhere it shouldn't (pasted in chat, committed to a public repo, etc.),
  rotate it at the provider's console immediately — don't just remove it from wherever it leaked.

## Get a free Groq API key

Groq is the default AI provider this project ships pointed at, and it has a genuine permanent
free tier — no credit card required to sign up or to generate a key.

### ⚠️ Free tier only

Your API key lives in a plaintext config file (`config.yml`) on whatever machine runs your
Minecraft server. If that hosting is ever compromised, shared with other tenants, or accessible
to anyone besides you — which is common on cheap/shared game hosting — **that key can leak**.

- A **free-tier key with no payment method attached** is low-stakes if it leaks: worst case,
  someone burns your free rate limit and you rotate the key. No bill.
- A **paid key or one with a card attached** is a real financial risk if it leaks: someone could
  run up usage charges before you notice.

So: create a free Groq account, don't add a payment method to it, and use only that key here.

### Steps

1. Go to **[console.groq.com](https://console.groq.com)** and sign up (Google/GitHub/email — no
   card asked for).
2. Go to **[console.groq.com/keys](https://console.groq.com/keys)**.
3. Click **Create API Key**, give it any name (e.g. `minecraft-server`), and copy the key it shows
   you — Groq only shows the full key once.
4. Open your server's `config.yml` and paste it in:
   ```yaml
   ai:
     provider: "openai-compatible"   # already the default — Groq speaks this wire format
     apiKey: "gsk_...paste your key here..."
     baseUrl: "https://api.groq.com/openai/v1"
     models:
       - "groq/compound"
       - "openai/gpt-oss-120b"
       - "openai/gpt-oss-20b"
   ```
   (Groq's exact model lineup changes over time — check
   [console.groq.com/docs/models](https://console.groq.com/docs/models) for the current list if
   the ones above ever stop working. Skip anything named `prompt-guard` — those are small
   input-safety classifiers, not chat models.)
5. Run `/aicompanion reload` in-game (or restart the server).

### If it leaks

Rotate it immediately at [console.groq.com/keys](https://console.groq.com/keys) — delete the old
key and create a new one. Don't just remove it from wherever it leaked; assume it's already been
seen.

### Never

- Never commit `config.yml` with a real key in it to a public repo.
- Never hand out a jar or config with your key baked in — every server install should generate
  and use its own key.
- Never attach a payment method to the account this key belongs to.
