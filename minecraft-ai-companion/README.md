# AI Companion — multi-platform plan

Port of `ai-bot-openrouter/watcher.js` (a Node.js sidecar that tailed the PaperMC log and used
RCON) into native server plugins/mods. Build order: **Paper** (done) → **Fabric** (1.21.11 done,
26.1 blocked — see below) → Forge/NeoForge (not started).

**Server-agnostic by design:** the original `watcher.js` shipped with one specific server's
lore/rules baked into its KB file. Nothing here is patterned to any particular server — every bit
of server-specific content (name, rules, commands, lore, item lists, whatever) is admin-supplied
config, and every default that ships in the jar is a generic, empty-ish template meant to be
replaced, not real content for a real server.

## common/ — shared logic, used by the Fabric modules

Plain Java, zero Minecraft/Bukkit/Fabric imports (verified by grep, not just by design): the AI
provider client, KB chunk search, rate limiting, conversation memory, ask-queue batching, YAML
config loading, and the full `AskProcessor` orchestration (prompt construction, parsing, the
whole join/ask flow) — abstracted from any specific platform via a small `GameBridge` interface
(give an item, send a chat line, resolve a rate-limit key) that each Fabric MC-version module
implements once in its own mapped API. Built with Maven, installed to the local repo (`mvn
install`) so Gradle-based Fabric modules can depend on it via `mavenLocal()`.

Deliberately **not** wired into `paper/`, which still carries its own copies of these same
classes — `paper/` was already built and released before this module existed; retrofitting a
working, shipped platform to depend on it is a separate, lower-risk-when-done-later step, not
bundled into standing this module up for Fabric.

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
      1.21.11/
        ai-companion-fabric-1.0.0.jar
      26.1/                                    <- once fabric-26.1's build is unblocked
        ai-companion-fabric-1.0.0.jar
    forge/
      1.21.11/
        ...
      26.1/
        ...
```

**Module naming convention `release.sh` reads to build this automatically:**
- `paper` — no MC-version suffix, one jar, filed straight under `<platform>/`.
- `fabric-1.21.11`, `fabric-26.1`, `forge-26.1`, etc. — `<platform>-<mc-version>`, each its own
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

Both Maven modules (Paper) and Gradle/Loom modules (Fabric) build automatically — `release.sh`
detects which build tool a module uses and drives it accordingly. A Gradle module needs its own
wrapper (`./gradlew`) generated before `release.sh` will touch it (`gradle wrapper
--gradle-version <ver>` run once inside that module) — if a module can't configure successfully
yet (see `fabric-26.1/` above), it can't generate a wrapper either, so `release.sh` just skips it
with a reminder instead of guessing.

`releases/` is build output like `target/`/`build/` — regeneratable from source at any time — so
if this project ever moves into git, all three belong in `.gitignore` rather than being committed;
keep specific jars you want to hand out by copying them elsewhere. `release.sh` will happily
rebuild any past version if you check out that version's source and run it again.

## fabric-1.21.11/ — done, builds clean

A real Fabric mod: `ServerPlayerEvents.JOIN`/`ServerMessageEvents.ALLOW_CHAT_MESSAGE` for
join/chat (the latter *cancels* a message that will never be answered — duplicate/cooldown/full —
instead of letting spam show in public chat, same anti-spam behavior as Paper), items given by
constructing an `ItemStack` from the vanilla registry directly (no command-dispatch trick needed —
Fabric doesn't have Bukkit's ecosystem of plugins overriding vanilla commands), `/aicompanion
reload` via Brigadier. Config is `config/aicompanion/config.yml` (same YAML shape/keys as Paper's
`config.yml` — copy one to the other) parsed with a bundled snakeyaml, since Fabric has no
Bukkit-style built-in config API.

Built with **Yarn mappings** (`1.21.11+build.6`) + **Java 21**, via Fabric Loom. Has its own Gradle
wrapper (`./gradlew build` from inside `fabric-1.21.11/`) so it doesn't depend on any
pre-installed Gradle. `release.sh` builds it like any other module.

One real API surprise worth knowing if you touch this code: **1.21.11 replaced Minecraft's old
integer op-levels (0–4) with a named `PermissionLevel` enum** (`ALL`/`MODERATORS`/`GAMEMASTERS`/
`ADMINS`/`OWNERS`) — `CommandManager.requirePermissionLevel(CommandManager.ADMINS_CHECK)` is the
modern equivalent of the old `.requires(source -> source.hasPermissionLevel(4))`.

## fabric-26.1/ — source complete, build currently blocked upstream

The Java source is fully written and extensively cross-checked (via `javap` against real,
correctly-mapped Minecraft classes) — but it can't actually compile right now because **Mojang is
not currently publishing official mapping data for the 26.x version line** (confirmed directly
against `piston-meta.mojang.com`'s live version manifest: every 26.1.x patch and 26.2 have a
`downloads` block with only `client`/`server`, no `client_mappings`/`server_mappings` — unlike
1.21.11, which has both). Fabric's own Yarn mappings are built *from* Mojang's official mappings,
which is also why no Yarn build exists for this line either (see `fabric-26.1/gradle.properties`).

This looks like it may be temporary, not a permanent policy change: a separate project on this
machine has a genuinely successful build from **August 23rd** with correctly-mapped classes
cached, and Mojang's own manifest for 26.1.2 shows a `time` of **August 25th** — meaning mappings
were very likely available up to right around when Mojang last regenerated that version's
manifest, and the regen is probably what dropped them. Worth periodically retrying rather than
treating this as permanently stuck:

```
cd fabric-26.1
JAVA_HOME=<path to a Java 25 JDK> gradle --no-daemon build
```

(needs a system/portable Gradle since this module has no wrapper yet — generating one requires
the project to configure successfully first, which is exactly what's currently blocked). Once it
builds, run `gradle wrapper --gradle-version 9.7.1` inside it so `release.sh` picks it up
automatically like `fabric-1.21.11`.

Different mapping set, real naming differences worth knowing about if you pick this back up:
Mojang mappings use `ServerPlayer`/`MinecraftServer` (not Yarn's `ServerPlayerEntity`),
`net.minecraft.resources.Identifier` (not `ResourceLocation` — genuinely renamed in this version),
`BuiltInRegistries`/`Registry.getOptional(...)`, `Component`/`ChatFormatting`, and the same
op-level-to-`PermissionLevel` change as 1.21.11 but with different names:
`Commands.hasPermission(Commands.LEVEL_ADMINS)`.

## forge/ — not started
