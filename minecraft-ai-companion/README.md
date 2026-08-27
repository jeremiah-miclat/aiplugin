# AI Companion — multi-platform plan

Port of `ai-bot-openrouter/watcher.js` (a Node.js sidecar that tailed the PaperMC log and used
RCON) into native server plugins/mods. Build order: **Paper first** (done), then Fabric, then
Forge/NeoForge.

**Server-agnostic by design:** the original `watcher.js` shipped with one specific server's
lore/rules baked into its KB file. Nothing here is patterned to any particular server — every bit
of server-specific content (name, rules, commands, lore, item lists, whatever) is admin-supplied
config, and every default that ships in the jar is a generic, empty-ish template meant to be
replaced, not real content for a real server.

## paper/ — done, builds clean

A real Paper plugin, not a sidecar: hooks `PlayerJoinEvent`/`AsyncChatEvent` directly and gives
items via the console command dispatcher in-process — no RCON, no log tailing. Full feature
parity with watcher.js: personality (friendly/trashtalk), KB chunk search, per-player +
server-wide conversation memory, batched AI calls with model fallback/rotation across a
configurable provider (OpenRouter/OpenAI-compatible/Anthropic — see SETUP.md), per-IP 24h
item-give limits, equipment quantity caps, crash-safe state persistence.

**Version strategy:** compiled against Paper API **1.21.11** (the last release before Mojang's
2026 switch to year.drop.hotfix versioning) targeting **Java 21** bytecode — deliberately not the
newer 26.x API / Java 25 baseline. Paper's API is additive across versions, so this jar should
load and run on a 26.x server too; going the other way would not (26.x-only calls would 404 on an
old server, and Java 25 bytecode can't load on 1.21.11's Java 21 JVM at all). If real testing on a
26.x server turns up an actual incompatibility, that's the point to add a version-gated code path.

**Build (one platform, for iterating):**
```
cd paper
mvn package
```
Output: `paper/target/ai-companion-paper-<version>.jar` (shaded, Gson bundled/relocated) — but
`target/` is Maven build output, wiped/regenerated on every build, so it's not where a jar you
mean to keep should live. See **Releases** below for that.

**Install and configure:** drop the jar in `plugins/`, start the server once to generate
`plugins/AiCompanion/config.yml` + `server-info.md` + `kb/`, then see **[SETUP.md](SETUP.md)** —
the single source of truth for configuring this project on any platform (AI provider setup with
free-tier options, describing your own server, item-giving limits, the reload command, every
config key). Don't duplicate that content here; if this README and SETUP.md ever disagree,
SETUP.md is the one that's right.

Not carried over from the original `config.json`: `logPath`/`pollIntervalMs` (no log tailing
anymore) and the `rcon` block (gives go straight through Bukkit in-process). Don't reuse the old
RCON password or OpenRouter key from `ai-bot-openrouter/config.json` — treat that file as
compromised/rotate the key, since it was sitting in plaintext.

## Releases

Every version, of every platform (and, where it applies, every targeted Minecraft version), gets
its own folder — `releases/<version>/<platform>/[<mc-version>/]<jar>` — so past builds stay around
as a real archive instead of being overwritten by whatever a build tool's own output dir (`target/`,
`build/libs/`) last produced.

**Why the extra `<mc-version>` level, and why Paper doesn't use it:** Paper deliberately ships ONE
jar that spans multiple Minecraft versions on purpose (see the version-strategy note above — built
against the older 1.21.11 API, which is forward-compatible with 26.x). Fabric and Forge/NeoForge
don't get that option: those loaders tie a build tightly to one specific Minecraft version's
mappings/API, so supporting both 1.21.11-era and 26.x means genuinely separate builds — separate
module directories, each its own complete project, each producing its own jar. Layout once those
exist:

```
releases/
  1.0.0/
    paper/
      ai-companion-paper-1.0.0.jar            <- one jar, spans MC versions
    fabric/
      1.21.1/
        ai-companion-fabric-1.0.0.jar
      26.1/
        ai-companion-fabric-1.0.0.jar
    forge/
      1.21.1/
        ...
      26.1/
        ...
```

**Module naming convention `release.sh` reads to build this automatically:**
- `paper` — no MC-version suffix, one jar, filed straight under `<platform>/`.
- `fabric-1.21.1`, `fabric-26.1`, `forge-26.1`, etc. — `<platform>-<mc-version>`, each its own
  complete module (own `pom.xml`/`build.gradle`), filed under `<platform>/<mc-version>/`. Add a
  new module directory per Minecraft version you want to support; the script picks it up with no
  other configuration.

**Cutting a release:**
1. Bump `<version>` in the module's build file (e.g. `paper/pom.xml`). That's the single source of
   truth for that module's version — Paper's `plugin.yml` pulls its `version:` line from it
   automatically at build time via Maven resource filtering, nothing else to keep in sync by hand.
2. From `minecraft-ai-companion/`, run `./release.sh`.
3. It discovers every module directory matching the naming convention above, builds it, and copies
   the real jar (not a shade plugin's `original-*`/`-shaded.jar` intermediates) into the right
   `releases/<version>/<platform>/[<mc-version>/]` folder.

Modules can be at different versions independently (a Paper-only bugfix release doesn't have to
bump Fabric/Forge) — `release.sh` reads each module's own version, so it files into whichever
version folder is actually its own.

Maven modules (Paper) build automatically today. Fabric/Forge will use Gradle + Loom once they
exist, which `release.sh` doesn't drive yet on purpose — that logic is only worth writing (and
testing for real) against an actual module, not guessed in advance; it'll print a reminder to
build those manually until that support is added.

`releases/` is build output like `target/`/`build/` — regeneratable from source at any time — so
if this project ever moves into git, all three belong in `.gitignore` rather than being committed;
keep specific jars you want to hand out by copying them elsewhere. `release.sh` will happily
rebuild any past version if you check out that version's source and run it again.

## fabric/ and forge/ — not started

Deliberately not scaffolded yet per the build order. Note going in: Fabric and Forge/NeoForge are
mod loaders (client+server), not server-plugin APIs like Paper/Bukkit — there's no shared code
with `paper/` beyond plain data/logic classes (AiClient, AskParser, KnowledgeBase, RateLimiter,
ConversationMemory all ported as-is with zero Bukkit imports, so they can likely be reused
directly). The event hookup, item-giving, and chat-broadcast layers will need separate Fabric and
Forge implementations using each loader's own server-join/chat-message events and
`ServerPlayerEntity` item-giving APIs.
