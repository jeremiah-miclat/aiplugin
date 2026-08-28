# AI Companion — multi-platform plan

Port of `ai-bot-openrouter/watcher.js` (a Node.js sidecar that tailed the PaperMC log and used
RCON) into native server plugins/mods. Build order: **Paper** (done) → **Fabric** (1.21.11, 26.1
done) → **Forge** (1.21.11, 26.1, 26.2 done) → **NeoForge** (1.21.11, 26.1, 26.2 done). Every
platform is now covered; see the `fabric-26.1/` section below for why the 26.x line looked blocked
at first and wasn't.

**Server-agnostic by design:** the original `watcher.js` shipped with one specific server's
lore/rules baked into its KB file. Nothing here is patterned to any particular server — every bit
of server-specific content (name, rules, commands, lore, item lists, whatever) is admin-supplied
config, and every default that ships in the jar is a generic, empty-ish template meant to be
replaced, not real content for a real server.

## common/ — shared logic, used by the Fabric, Forge, and NeoForge modules

Plain Java, zero Minecraft/Bukkit/Fabric/Forge/NeoForge imports (verified by grep, not just by
design): the AI provider client, KB chunk search, rate limiting, conversation memory, ask-queue
batching, YAML config loading, and the full `AskProcessor` orchestration (prompt construction,
parsing, the whole join/ask flow) — abstracted from any specific platform via a small `GameBridge`
interface (give an item, send a chat line, resolve a rate-limit key) that each Gradle module
implements once in its own mapped API. Built with Maven, installed to the local repo (`mvn
install`) so the Gradle-based Fabric/Forge/NeoForge modules can depend on it via `mavenLocal()`.

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
      26.1/
        ai-companion-fabric-1.0.0.jar
    forge/
      1.21.11/
        ai-companion-forge-1.0.0.jar
      26.1/
        ai-companion-forge-1.0.0.jar
      26.2/
        ai-companion-forge-1.0.0.jar
    neoforge/
      1.21.11/
        ai-companion-neoforge-1.0.0.jar
      26.1/
        ai-companion-neoforge-1.0.0.jar
      26.2/
        ai-companion-neoforge-1.0.0.jar
```

**Module naming convention `release.sh` reads to build this automatically:**
- `paper` — no MC-version suffix, one jar, filed straight under `<platform>/`.
- `fabric-1.21.11`, `fabric-26.1`, `forge-26.1`, `neoforge-26.2`, etc. — `<platform>-<mc-version>`, each its own
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
yet, it can't generate a wrapper either, so `release.sh` just skips it with a reminder instead of
guessing.

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

## fabric-26.1/ — done, builds clean

Was blocked, now fixed — the real root cause turned out to have nothing to do with Mojang's
mapping data being unavailable (that theory, recorded below for the record, was wrong).
**Minecraft ships genuinely unobfuscated as of 26.1** (confirmed against Fabric's own
announcement, https://fabricmc.net/2026/03/14/261.html, and against
`FabricMC/fabric-example-mod`'s `26.1.2` branch), so there's no mapping step at all for this
version line — and Fabric ships a **separate Gradle plugin id** for that, `net.fabricmc.fabric-loom`,
not a new capability on the old `fabric-loom` id. This module's `build.gradle` was requesting
newer/SNAPSHOT *versions* of the old plugin id, which could never work regardless of version since
that plugin's mapping-resolution code always expects Mojang's `client_mappings`/`server_mappings`
manifest fields to exist (confirmed directly: even the newest `1.18.0-alpha.19` build of the old
`fabric-loom` id still throws `Failed to find official mojang mappings for 26.1.2`). Switching the
plugin id fixed it outright — first build under the new id got all the way to a real, single
compile error (`PlayerChatMessage.getContent()` doesn't exist any more — `signedContent()` does)
rather than failing at configuration time.

Practical notes if you touch this module:
- No `mappings` line in `build.gradle` at all — nothing to configure.
- Dependencies that used to need `modImplementation` (fabric-loader, fabric-api) now just use
  plain `implementation`, since there's no remap step distinguishing "mod" dependencies from
  ordinary ones. `include(implementation(...))` for jar-in-jar (common/snakeyaml) is unchanged.
- Needs `org.gradle.configuration-cache=false` in `gradle.properties` — this Loom version isn't
  compatible with Gradle's configuration cache yet (per `FabricMC/fabric-loom#1349`).
- **The Gradle daemon process itself must run on Java 25**, not just the compile toolchain — this
  is a real, separate requirement from Forge's 26.x modules (which don't need this: ForgeGradle
  provisions Java 25 internally just for Minecraft processing, without needing the outer daemon on
  25). Fixed here via `./gradlew updateDaemonJvm --jvm-version=25`, which writes
  `gradle/gradle-daemon-jvm.properties` — the wrapper then self-selects/auto-downloads a Java 25
  JDK via the `foojay-resolver-convention` plugin already in `settings.gradle`, with no `JAVA_HOME`
  override needed on the machine running `release.sh`.

Different mapping set from Yarn, real naming differences worth knowing about: Mojang's own names
use `ServerPlayer`/`MinecraftServer` (not Yarn's `ServerPlayerEntity`),
`net.minecraft.resources.Identifier` (not `ResourceLocation`), `BuiltInRegistries`/
`Registry.getOptional(...)`, `Component`/`ChatFormatting`, and the same op-level-to-`PermissionLevel`
change as 1.21.11 but with different names: `Commands.hasPermission(Commands.LEVEL_ADMINS)` — all
confirmed identical to what `forge-1.21.11/`, `forge-26.1/`, and `forge-26.2/` already document,
since they're the same official Mojang mapping data Forge uses directly.

<details>
<summary>For the record: what the blocker looked like before it was understood (kept so a future
mis-diagnosis doesn't repeat this detour)</summary>

It looked, from the outside, exactly like missing mapping data: `piston-meta.mojang.com`'s live
version manifest for every 26.1.x patch and 26.2 has a `downloads` block with only
`client`/`server`, no `client_mappings`/`server_mappings` (unlike 1.21.11, which has both), and the
old `fabric-loom` plugin id's error message — `Failed to find official mojang mappings for
26.1.2` — reads exactly like "the data isn't there yet." Both of those observations are still
literally true; they just don't mean what they first appeared to mean. The actual reason those
manifest fields are empty is that there's nothing to map — Mojang stopped obfuscating the
client/server jars for this version line, so a mapping file would be a no-op. Forge's own tooling
handles that gracefully (its build log shows a `srg2names[...][Empty]` step and just proceeds); the
old Fabric Loom plugin id's mapping-resolution code doesn't have that fallback and just throws.
</details>

## forge-1.21.11/ — done, builds clean

A real Forge mod: `PlayerEvent.PlayerLoggedInEvent`/`ServerChatEvent` for join/chat (the latter
*cancels* the chat event — the inverse of Fabric's `ALLOW_CHAT_MESSAGE`, which instead returns
`true` to let a message through — for a message that will never be answered — duplicate/cooldown/
full — same anti-spam behavior as Paper/Fabric), items given by constructing an `ItemStack` from
the vanilla registry directly and inserting it into the player's inventory (mirrors vanilla's own
GiveCommand — drop whatever doesn't fit), `/aicompanion reload` via Brigadier. Config is
`config/aicompanion/config.yml` (same YAML shape/keys as Paper/Fabric's `config.yml` — copy one to
the other) parsed with the same bundled snakeyaml as the Fabric modules.

Built with **official Mojang mappings** (ForgeGradle resolves these itself — no separate mappings
project the way Fabric needs Yarn) + **Java 21**, against **Forge 1.21.11-61.2.0** via
ForgeGradle 7. Has its own Gradle wrapper (`./gradlew build` from inside `forge-1.21.11/`).
`release.sh` builds it like any other module.

**Bundling common/snakeyaml:** unlike the Fabric modules (which use Loom's built-in Jar-in-Jar),
this module uses the `com.gradleup.shadow` plugin against a dedicated `shade` Gradle configuration
(not the full runtime classpath — that would shade the whole of Minecraft/Forge into the jar) —
Forge's own Jar-in-Jar mechanism is meant for mod-to-mod dependencies with version ranges, not
plain libraries. Gson is excluded from that `shade` configuration for the same reason the Fabric
modules keep it `compileOnly`: Minecraft already bundles Gson on the runtime classpath.

Real API surprises worth knowing if you touch this code (all confirmed directly against the real,
official-mapped Forge 1.21.11-61.2.0 classes via `javap`, not assumed):
- **`ResourceLocation` is already renamed to `Identifier`** as of this build (`1.21.11-61.2.0`,
  August 2026) — contrary to what the `fabric-26.1/` section below says about the rename being
  26.x-only; that note was accurate for an earlier `1.21.11` patch's mapping data, evidently not
  this one.
- **Every Forge event type is its own static `EventBus<T>`** (`SomeEvent.BUS.addListener(...)`),
  not a single shared `MinecraftForge.EVENT_BUS` — a bigger eventbus rework than just the
  op-level/`PermissionCheck` rename both Fabric modules already document. Mod-lifecycle events
  (`FMLCommonSetupEvent` etc.) are the one exception, needing a mod-scoped `BusGroup` via
  `SomeEvent.getBus(modBusGroup)` instead, since those fire once per mod rather than once globally
  — not used here, since this mod does all its registration directly in the `AiCompanionMod`
  constructor rather than deferring to a setup event.
- `ServerChatEvent.BUS.addListener(Predicate<ServerChatEvent>)` — returning `true` **cancels**
  (suppresses) the event, the opposite sense of Fabric's `ALLOW_CHAT_MESSAGE`.
- `com.mojang.authlib.GameProfile` is a record now — `.name()`, not `.getName()`.
- `Player.drop(ItemStack, boolean)` is two-arg here (not three, unlike the `fabric-26.1/` draft's
  assumption for a later MC version) — mirrors vanilla's `GiveCommand` drop-what-doesn't-fit path.

## forge-26.1/ and forge-26.2/ — done, build clean

Same source as `forge-1.21.11/` — `AiCompanionMod.java`/`ForgeGameBridge.java` are byte-for-byte
copies, and both modules compiled against their respective Minecraft/Forge jars with **zero source
changes needed**. That's a real, compiler-verified result (not an assumption carried over): the
whole point of building these two out was to check whether the API surface documented above for
1.21.11 actually holds across the 26.x line, and it does, down to `Identifier`,
`GameProfile.name()`, `Player.drop(ItemStack, boolean)`, and the per-event-type `EventBus` model,
all unchanged.

What *does* differ, purely in build configuration:
- **Java 25**, not 21 — Mojang ships Java 25 to end users in 26.1+ (this is stated directly in
  Forge's own MDK `build.gradle` comment for this line, not guessed).
- `forge-26.1/` targets the **latest 26.1.x patch** (`26.1.2-64.1.0` as of scaffolding), the same
  way `forge-1.21.11/` targets 1.21.11's latest patch rather than every `-rc`/`-pre` along the way —
  update `gradle.properties` as new 26.1.x patches land rather than adding yet another module per
  dot-release.
- `forge-26.2/` is genuinely a separate Forge build line (`65.x`, vs. `64.x` for 26.1.2), so it gets
  its own module rather than being folded into `forge-26.1/`.
- `mods.toml`'s `minecraft` dependency range is `[26.1,26.2)` / `[26.2,26.3)` respectively, and
  `loaderVersion`/`forge` dependency ranges are `[64,)` / `[65,)` — these two jars will refuse to
  load on each other's Minecraft version even though the code is identical, which is correct: see
  the note on `srg2names[...][Empty]` below for why "identical code" doesn't imply "one jar could
  serve both" the way Paper's does across versions.

Building these two modules is what turned up the real explanation for `fabric-26.1/`'s blocker
(see that section above, now fixed): Mojang's `piston-meta.mojang.com` manifest for 26.2 has no
`client_mappings`/`server_mappings` entries not because that data is missing/delayed, but because
the 26.x client/server jars ship unobfuscated — there's nothing to map. Forge's build log for these
two modules shows that directly: a `srg2names[official-26.2][Empty]` step with no separate
mapping-file download at all, and it just proceeds. That's also exactly why `fabric-26.1/` needed a
different Fabric Loom *plugin id* (`net.fabricmc.fabric-loom`, not a newer version of the old
`fabric-loom`) rather than any change on the Forge side — see that section for the fix.

## neoforge-1.21.11/ — done, builds clean

A real NeoForge mod: registers on `NeoForge.EVENT_BUS` for `ServerStartedEvent`/`ServerStoppingEvent`
(state load/save, same dedicated-scheduler batch window as Fabric — `AskProcessor` never touches
world/player state directly, only via `NeoForgeGameBridge`), `PlayerEvent.PlayerLoggedInEvent` for
join, and `ServerChatEvent` for chat (cancelable — same accept/cancel-on duplicate/cooldown/full
behavior as Fabric's `ALLOW_CHAT_MESSAGE`, canceling a message that will never be answered instead
of letting spam show in public chat). `/aicompanion reload` via `RegisterCommandsEvent` + Brigadier.
Items are given by constructing an `ItemStack` from the vanilla registry directly, same as Fabric —
no `/give`-command trick needed. Config is `config/aicompanion/config.yml` (same YAML shape/keys as
Paper's and Fabric's — copy from either) parsed with a bundled snakeyaml.

Built with **official Mojang mappings** (NeoForge doesn't use Yarn) + **Java 21**, via
**ModDevGradle** (`net.neoforged.moddev`) — the NeoForge equivalent of Fabric Loom. Non-mod library
dependencies (`ai-companion-common`, snakeyaml) are bundled via NeoForge's `jarJar` mechanism,
NeoForge's equivalent of Loom's `include(...)`. Has its own Gradle wrapper (`./gradlew build` from
inside `neoforge-1.21.11/`) so it doesn't depend on any pre-installed Gradle. `release.sh` builds it
like any other module.

Being Mojang-mapped rather than Yarn-mapped, this module's class/method names match what
`forge-1.21.11/`'s section above documents in detail (`ServerPlayer`, `MinecraftServer`,
`Identifier`, `BuiltInRegistries`, `Component`/`ChatFormatting`) — same underlying Mojang mapping
data, different loader — including the same op-level-to-`PermissionLevel` change:
`.requires(Commands.hasPermission(Commands.LEVEL_ADMINS))` is the modern equivalent of the old
"requires op level 4" check.

One real, verified divergence from Forge, though: NeoForge kept a single shared `NeoForge.EVENT_BUS`
for game events — `ServerStartedEvent`/`ServerStoppingEvent`/`PlayerLoggedInEvent`/`ServerChatEvent`/
`RegisterCommandsEvent` all register via `NeoForge.EVENT_BUS.addListener(...)` — unlike Forge's
per-event-type static `EventBus<T>` model documented above. Confirmed by this module's own
successful build, not assumed from the two projects' shared lineage.

## neoforge-26.1/, neoforge-26.2/ — done, build clean

Same code, structure, and event wiring as `neoforge-1.21.11/` (see above) — `AiCompanionMod` and
`NeoForgeGameBridge` are near-identical across all three NeoForge modules, since `AskProcessor`
does all the real logic and the NeoForge event/command API turned out to be stable across 1.21.11 →
26.1 → 26.2. Two real deltas *did* turn up building against the actual decompiled classes (`javap`
against `build/moddev/artifacts/neoforge-<version>.jar` after a build, not guessed):

- **Giving items:** 1.21.11 has `Inventory.placeItemBackInInventory(ItemStack)`. That method's gone
  by 26.1 — instead it's `boolean absorbedAll = player.getInventory().add(stack); if
  (!absorbedAll && !stack.isEmpty()) player.drop(stack, false, false);`, mirroring vanilla's own
  `GiveCommand` (insert into inventory, drop whatever doesn't fit).
- **Rate-limit key:** 1.21.11 needs `player.connection.getConnection().getRemoteAddress()`. By 26.1
  the extra hop is gone — `player.connection.getRemoteAddress()` directly.

Both target **Java 25** (Mojang ships Java 25 to end users starting with the 26.x line, vs. 1.21.11's
Java 21) via NeoForge `26.1.2.99` / `26.2.0.69` respectively — no Parchment mappings configured for
either (not published for this line yet; per the `fabric-26.1/` section above, 26.x ships genuinely
unobfuscated, so there may simply be less for Parchment's parameter-name layer to add on top of
Mojang's own names — not investigated further since it isn't blocking anything here).
`settings.gradle` adds the `org.gradle.toolchains.foojay-resolver-convention` plugin so Gradle
auto-downloads a Java 25 toolchain on demand rather than needing one pre-installed (this machine
only has Java 21 on `PATH`, and the build worked anyway).
