# Modrinth "Content disclosures" field

Paste this into Modrinth's content disclosure section (the "Check content disclosures" checklist
item). It's written to stand alone, since Modrinth shows disclosures separately from the main
description.

---

**This project connects to a third-party AI API.**

To generate chat replies, this plugin/mod sends the text of player questions (and short server
context you write yourself — no personal player data) to an AI provider's API. By default that
provider is **Groq**; the config also supports OpenAI-compatible endpoints, OpenRouter, and
Anthropic if you choose to switch. You must supply your own API key from that provider — none is
bundled, and the plugin/mod does nothing until you add one.

**Use a free-tier key only — never a paid or credit-card-backed key.**

The API key is stored in plaintext in your server's `config.yml`. That file lives on whatever
hosting runs your server, and server hosting — especially cheap or shared hosting — can be
compromised, or accessible to other tenants/operators with filesystem access. If a key leaks:

- A **free-tier key with no payment method attached** costs you nothing worse than hitting a rate
  limit; rotate it and move on.
- A **paid key, or one with a card attached**, can be used to run up real charges before you
  notice.

Every AI provider option documented for this project (Groq, OpenRouter's `:free` models, local
Ollama) has a genuine free tier that needs no card. Set one of those up — see the bundled
[Groq API key guide](groq-api-key-guide.md) for the default path — and don't attach billing to
that account.

**Optional Discord relay.** If you turn on the optional Discord webhook relay, chat lines get
posted to a Discord channel via a webhook URL you provide. This is off by default and, like the AI
key, is a credential you supply and control — treat a webhook URL with the same care (don't share
it, rotate it if it leaks).

No player data leaves your server other than the chat text needed to generate a reply, sent
directly to the AI provider you configured.
