# Full setup guide

Everything you need to install, configure, and run AI Companion, on any of the four supported
platforms. This is the version of `SETUP.md` written for the Modrinth project page — paste it
into a "Full Guide" section of your description, host it as a wiki page, or just leave it here in
the repo and link to it.

## 1. Install

| Platform | Where the jar goes | Files generated on first start |
|---|---|---|
| Paper | `plugins/` | `plugins/AiCompanion/config.yml`, `server-info.md`, `kb/` |
| Fabric | `mods/` | `config/aicompanion/config.yml`, `server-info.md`, `kb/` |
| Forge | `mods/` | `config/aicompanion/config.yml`, `server-info.md`, `kb/` |
| NeoForge | `mods/` | `config/aicompanion/config.yml`, `server-info.md`, `kb/` |

Pick the jar matching your loader **and** Minecraft version (1.21.11, 26.1, or 26.2 — Paper ships
one jar that covers all of them). Start the server once to generate the files above, then it won't
answer anything useful until you fill in two of them: an API key, and your own server info.

## 2. AI API key

**Use a free-tier account with no payment method attached — not a paid one.** This project's
config keeps `apiKey` as plain text in `config.yml` on your server's own filesystem; a free key
only risks hitting a rate limit if it ever leaks, a paid one risks a real bill. Every provider
option below has a genuine free tier that needs no card.

The shipped default is **Groq**, and it's the one this guide walks through step by step: see
**[groq-api-key-guide.md](groq-api-key-guide.md)**.

Set these four fields under `ai:` in `config.yml`:

```yaml
ai:
  provider: "openai-compatible"   # shipped default — see groq-api-key-guide.md
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

## 3. Server info

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

## 4. Item giving

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

## 5. Other tunables

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

## 6. Discord relay (optional)

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

## Security notes

- Use a **free-tier** API key and Discord webhook — see §2 above for why.
- Never commit `config.yml` with a real key in it anywhere, and never hand out a jar/config with
  your key baked in — every install needs its own.
- If a key ever ends up somewhere it shouldn't (pasted in chat, committed to a public repo, etc.),
  rotate it at the provider's console immediately — don't just remove it from wherever it leaked.
