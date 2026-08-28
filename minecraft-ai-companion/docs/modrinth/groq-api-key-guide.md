# Get a free Groq API key

Groq is the default AI provider this project ships pointed at, and it has a genuine permanent
free tier — no credit card required to sign up or to generate a key.

## ⚠️ Free tier only

Your API key lives in a plaintext config file (`config.yml`) on whatever machine runs your
Minecraft server. If that hosting is ever compromised, shared with other tenants, or accessible
to anyone besides you — which is common on cheap/shared game hosting — **that key can leak**.

- A **free-tier key with no payment method attached** is low-stakes if it leaks: worst case,
  someone burns your free rate limit and you rotate the key. No bill.
- A **paid key or one with a card attached** is a real financial risk if it leaks: someone could
  run up usage charges before you notice.

So: create a free Groq account, don't add a payment method to it, and use only that key here.

## Steps

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

## If it leaks

Rotate it immediately at [console.groq.com/keys](https://console.groq.com/keys) — delete the old
key and create a new one. Don't just remove it from wherever it leaked; assume it's already been
seen.

## Never

- Never commit `config.yml` with a real key in it to a public repo.
- Never hand out a jar or config with your key baked in — every server install should generate
  and use its own key.
- Never attach a payment method to the account this key belongs to.
