# Setup Guide

This is the single source of truth for configuring the AI Companion, on any platform (Paper,
Fabric — see the project [README](README.md) for which Minecraft versions each currently
supports and where to get/build the jar). Every platform uses the same `config.yml` shape/keys —
copy one platform's config to another and it'll just work. If something here ever disagrees with
a comment in a shipped `config.yml`, this file is the one to trust — config.yml comments
summarize, this explains.

## 1. Install

- **Paper:** drop the jar in `plugins/`, start the server once. Generates
  `plugins/AiCompanion/config.yml`, `server-info.md`, and a `kb/` folder.
- **Fabric:** drop the jar in `mods/`, start the server once. Generates
  `config/aicompanion/config.yml`, `server-info.md`, and a `kb/` folder.

Either way, the plugin/mod won't answer questions usefully until you touch at least two of those
files: an API key, and your own server info.

## 2. Pick an AI provider and get a FREE key

**Use a free-tier account with no credit card attached, on whichever provider you choose — not a
paid one.** This is a real recommendation, not boilerplate: a free-tier key has a hard ceiling
(you get rate-limited or an error if you go over), while a paid/credit-backed key can turn a bug,
a spam flood, or a misconfigured limit into a real bill. Every option below has a genuine free
tier that doesn't require a card.

Set these four fields under `ai:` in `config.yml`:

```yaml
ai:
  provider: "..."     # which backend — see the three options below
  apiKey: "..."       # YOUR OWN key for that provider — never share one across installs
  baseUrl: "..."      # only used by "openai-compatible" — ignored otherwise
  models: [...]       # model ids that specific provider actually recognizes
```

### Option A — `"openai-compatible"` (the shipped default, pointed at Groq)

Covers Groq, OpenAI, Together, Fireworks, DeepSeek, Mistral, xAI, or a self-hosted Ollama/LM
Studio/vLLM server — anything speaking the same chat/completions wire format. `baseUrl` is the
part that changes per provider; `apiKey`/`models` still need to match whichever one you pick.

| Provider | Free tier, no card | Get a key | `baseUrl` | Example `models` |
|---|---|---|---|---|
| **Groq** (shipped default) | Yes | https://console.groq.com/keys | `https://api.groq.com/openai/v1` | `"groq/compound"`, `"openai/gpt-oss-120b"`, `"openai/gpt-oss-20b"`, `"qwen/qwen3.6-27b"` |
| OpenAI | Trial credit only, then paid | https://platform.openai.com/api-keys | `https://api.openai.com/v1` | `"gpt-4o-mini"` |
| Local Ollama | Yes (your own hardware) | — no key needed, `apiKey` can be any non-empty string | `http://localhost:11434/v1` | whatever you've pulled, e.g. `"llama3.1"` |

Groq's model lineup changes over time — check https://console.groq.com/docs/models for the
current list rather than trusting a stale copy of it. Skip anything named `prompt-guard` — those
are tiny input-safety classifiers, not chat models, and won't produce usable replies here.

### Option B — `"openrouter"`

One key, and OpenRouter itself gives you a choice of models from virtually every provider
(Anthropic, OpenAI, Google, Meta, etc.). Free key at https://openrouter.ai/keys. Stick to model
ids ending in `:free` to stay on the free tier — e.g. `"meta-llama/llama-3.3-70b-instruct:free"`.
`baseUrl` is ignored for this option.

### Option C — `"anthropic"`

Talks to Claude directly, not through OpenRouter. Free trial credit (not an ongoing free tier) at
https://console.anthropic.com/settings/keys — this is the one option above where "free" is
time/credit-limited rather than a permanent tier, worth knowing going in. `baseUrl` is ignored.
Example model id: `"claude-sonnet-5-20260101"` (check Anthropic's docs for the current one).

### After changing any of this

Run `/aicompanion reload` or restart the server. Pending questions, 24h item-give cooldowns, and
conversation history all survive a reload — only config.yml/server-info.md/kb/ get re-read.
Needs op-equivalent permission: on Paper that's the `aicompanion.reload` permission (default op,
alias `/aic`); on Fabric it's vanilla's permission level 4 (the same level `/op` grants).

## 3. Describe your own server

Nothing in this plugin is pre-loaded with any particular server's content — every default ships
as a template meant to be replaced, not real content.

- **`server-info.md`** — short, factual bullet points: server name, rules, gamemode, key
  commands/plugins, links. This gets pasted into every prompt, so keep it tight.
- **`kb/`** — a folder, not a single file. Drop in as many `.md` files as you want for anything
  `server-info.md` doesn't have room for (game systems, lore, item lists, event rules). Ships with
  one `01-overview.md` showing the format with a toy example — delete the example, write your own.
  Files are read in filename order (`01-...`, `02-...`) if order matters to you. Give things clear
  names in headings and **bold** text — that's what gets matched against a player's question.

Both are re-read live on every question — no reload needed for these two specifically.

## 4. Item giving

```yaml
itemGiving:
  enabled: true        # master switch — false means the bot NEVER gives an item, no exceptions
  maxPerDay: 10         # per player, rolling 24h window
  maxQuantity: 64        # ordinary stackable items
  maxEquipmentQuantity: 1 # weapons/armor/tools/etc (vanilla max stack size 1 anyway)
```

Turning `enabled: false` doesn't just stop gives from happening — the AI is told in its own prompt
never to offer one, and if it ever ignores that, the plugin overrides its reply with a plain
"item giving is off here" message rather than letting a promised-but-undelivered item slip through.

## 5. Other tunables

| Key | What it does |
|---|---|
| `botName` | the name shown in chat before every bot message, default `"mcAi"` |
| `personality` | `"friendly"` or `"trashtalk"` — the bot's tone |
| `askPrefix` | the chat prefix players type, default `"!ai"` |
| `batchWindowMs` | how often queued questions go out as one AI call (default 10s) |
| `maxAsksPerWindow` | total questions accepted per window, across all players |
| `askCooldownSeconds` | per-player pacing independent of `batchWindowMs` — see the comment in config.yml |
| `maxAskSubparts` | how many distinct requests one message can resolve at once |
| `broadcastReplies` | `true` = bot replies go to server-wide chat; `false` = console log only |

## 6. Mirror chat to Discord (optional)

Every bot chat line (join greetings, `!ai` replies) can be relayed into a Discord channel via an
incoming webhook — no DiscordSRV or other bridge plugin needed, works with any Discord server:

1. In Discord: the target channel's settings → Integrations → Webhooks → New Webhook → Copy
   Webhook URL.
2. In `config.yml`:
   ```yaml
   discord:
     webhookUrl: "https://discord.com/api/webhooks/..."
     username: ""   # optional — overrides the name Discord shows; blank uses the webhook's own
   ```
3. `/aicompanion reload` or restart.

Leave `webhookUrl` blank (the default) to keep this off — nothing is sent anywhere. A relay
failure (bad URL, Discord down, rate-limited) is logged and dropped; it never affects the in-game
reply that triggered it. Only actual chat lines are mirrored — per-player bookkeeping notices
(cooldown/queue-full messages) are not, since those aren't newsworthy outside the game.

Want it to ride through the exact channel/bot identity an existing DiscordSRV bridge already uses
instead of a separate webhook post? That's a deeper integration (DiscordSRV's own Java API) that
isn't built — ask if you want it added.

## Security notes

- Never commit `config.yml` with a real key in it anywhere, and never hand out a jar/config with
  your key baked in — see §2 above on why every install needs its own.
- If a key ever ends up somewhere it shouldn't (pasted in chat, committed to a public repo,
  etc.), rotate it at the provider's console — don't just remove it from wherever it leaked.
