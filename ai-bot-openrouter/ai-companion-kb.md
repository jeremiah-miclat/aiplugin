# Riftforged — Player Knowledge Base

This file is a reference for an in-game AI assistant feature to answer player questions about
the Riftforged server. It covers every player-facing command, every game system, the full boss
skill kits, and every craftable gear item (skill, effect, and exact crafting recipe). Content is
written to be read and quoted back to players — plain factual reference, no internal
implementation detail, no admin/OP-only commands.

Source of truth: this file is derived from the published player guide
(`docs/rpgplugin-encyclopedia.html`) and the plugin's actual recipe-generation code, so numbers
here should match what a player sees in-game. If a change is ever made to gear skills, boss
kits, or recipes, this file should be regenerated/updated to match.

---

## 1. What is Riftforged?

Riftforged is a Minecraft Java Edition MMORPG plugin — attributes, gear, companions, and boss
fights layered on top of vanilla Minecraft, running on a real multiplayer server.

- **Rifts are server-wide events.** Only one Rift can be active at a time — when one opens, it's
  not a private instance, it's the event the whole server converges on. Guards trickle in first,
  then a second wave, then the structure's boss lands in a full summoning ritual.
- **Bosses have real kits, not vanilla AI.** Each of the six boss types runs its own skill kit —
  telegraphed attacks you can dodge, and "mass seize" abilities that lock down everyone nearby at
  once. Scripted skill damage always lands (no evade-stat cheese); ordinary swings are still
  dodgeable.
- **Gear is crafted, not looted whole.** Winning a boss fight hands you the pieces — an
  Enchantment Stone, a shot at a crafting core, boss-specific materials — not a finished weapon.
  Take those to a crafting table and build one of 129 gear items, each with its own real skill.
  Then push it further at the Combine Forge: socket stones into it, unlock more sockets, level a
  stone up, repair it after it breaks, or Refine it for a permanent stat boost.
- **Elytra skins are a real archetype choice**, not a reskin: Speedster, Tank, or Glass Cannon,
  each with its own stat profile.
- **Deviants are companion pets.** Buy one, gear it up piece by piece exactly like your own
  equipment, and it fights beside you — no separate leveling grind, no mount/riding mechanic.
- **Attributes grow from vanilla XP.** Six stats — Strength, Agility, Dexterity, Vitality,
  Intelligence, Luck — level up off the XP you're already earning, with points spent any time via
  a GUI.
- **Still Minecraft.** None of this replaces vanilla — structures, mobs, and crafting are all
  still exactly where you left them, just with a lot more happening underneath.

---

## 2. Attributes (`/updatestats`)

Six stats drive everything you do in combat and survival:

| Stat | Governs |
|---|---|
| **Strength (STR)** | Flat damage — melee, ranged, and splash alike |
| **Agility (AGI)** | Attack speed + movement speed |
| **Dexterity (DEX)** | Bow/crossbow damage |
| **Vitality (VIT)** | Max health |
| **Intelligence (INT)** | Potion/healing power, and damage resistance to magic, poison, fire, and wither |
| **Luck (LUK)** | Crit chance |

You don't grind a separate XP bar for these — your RPG level rises off the vanilla XP you're
already earning, and every level gives you points to spend in `/updatestats`.

**Important: dying resets all six stats to zero.** The points themselves aren't lost forever —
you just have to reallocate them once you're back on your feet — but a first death stings more
than you'd expect because of this.

---

## 3. Player Commands

| Command | Aliases | What it does |
|---|---|---|
| `/riftboard` | — | Opens the Rift Board — browse every discovered rift (its level/boss/cleared status), and get a free tracking compass to it. |
| `/updatestats` | — | Opens a GUI to allocate STR/AGI/DEX/VIT/INT/LUK attribute points. |
| `/ascenditem` | — | Opens the Combine Forge GUI — Socket, Unlock, Level, Remove, Repair, and Refine modes for your gear. |
| `/deviant <buy\|equip\|guide>` | `/dv` | Buy and equip Deviants. Summon/despawn your Deviant by right-clicking its skull item; shift-right-click to manage it. |
| `/deviantduel <player>` | — | Challenge another player's held Deviant to a 1v1 duel. |
| `/deviantduelaccept <requester>` | — | Accept a pending Deviant duel request. |
| `/elytragear` | — | Opens the elytra quick-swap loadout GUI (1 World Boss elytra slot + 10 firework rocket slots). Offhand rockets auto-refill from here while gliding; sneak + left-click the ground to instantly equip the stored elytra. |
| `/worldbossguide` | `/wbg`, `/bossguide` | Opens a GUI guide to every World Boss — stats, skills, and how to beat them. |
| `/gearcraft` | — | Opens a browsable GUI of every craftable gear item and elytra skin — each entry shows its exact ingredients with live have/need counts against your inventory; click one to instantly craft it if you have everything. |

---

## 4. Rifts / Territory Events

**Finding one:** `/riftboard` lists every structure that's ever produced a rift — its level, which
boss it summons, whether it's been cleared, and when. Click an entry for a free tracking compass
straight to it. A rift's portal is also physically there in the world once it exists — a
swirling vortex with its own ambient sound, louder/more active while a fight is underway. If
nothing's listed yet, go fight near one of the game's eligible structures instead — villages,
pillager outposts, ruined portals, ancient cities, and more all qualify.

**Opening one:** Rifts open on purpose, not by chance. Build a closed rectangular frame (emerald
blocks, by default) near one of the eligible structures, then right-click the frame with Flint
and Steel. Get the shape right and the frame lights up on the spot — it stays exactly the blocks
you placed, just with a steady drift of portal particles marking it as live. **Only one rift can
exist on the entire server at a time** — a fresh frame won't do anything until whatever's
currently active is cleared or abandoned; build it early if you like, it'll simply wait. A
built frame never disappears on its own, even after that rift is cleared or fails — it stays
lit, ready for a fresh attempt (the world remembers how tough it was last time and adjusts from
there). Walking into a lit frame gives you a 10-second countdown before you're pulled in — step
away before it finishes to cancel.

**Inside the event:** Stepping through the portal pulls you into a private copy of the
structure — nothing you do there touches the real world, and nothing can be taken from it
either (no placing blocks, no breaking anything but loose grass/plants, no buckets, no boats, no
opening chests/barrels while inside). Combat doesn't start until the first player actually steps
through — guards then spawn in escalating waves, sometimes armed with real weapons. After two
waves, the structure's boss lands in a full summoning ritual with its own escort. Wander too far
from the portal's center and you're pulled back to reality; if the copy empties out entirely,
the rift fails and resets.

**The boss fight:** Every boss type has its own kit — telegraphed attacks you can see coming and
dodge, and periodic "mass seize" abilities that mark and lock down everyone nearby at once, so
don't fight one solo if you can help it. See the full Boss Guide (section 8) for every boss's
exact abilities, cooldowns, and how to dodge/counter them.

**Loot:** Killing the boss spawns a loot chest back in the real world, right where the structure
stands — emeralds, a Totem of Undying, and an Enchantment Stone, free for whoever gets there
first. On top of that, **everyone who damaged the boss** gets their own personal reward the
instant it dies: an Enchantment Stone and a Gear Core, dropped as glowing, owner-locked items
only they can pick up. You have 60 seconds after the kill before the rift pulls you back to
reality, so grab what's yours before then. None of this is a finished weapon yet — it's what you
build one from.

---

## 5. Gear: Crafting and the Combine Forge

**Crafting:** Take your materials to a normal crafting table. Every one of the 129 gear items has
a fixed recipe built from a crafting core (the "World Boss Heartstone"), materials from **two
different bosses**, and some currency ore — so a full build means fighting more than one boss
type. Each item comes out with its own unique skill already active, with open sockets to fill
later. Not sure what a recipe needs? `/gearcraft` shows every recipe's exact ingredients with
live have/need counts, and crafts instantly on click if you have everything.

**Dying breaks your gear.** Every gear item you're carrying — equipped or not — shatters the
moment you die, and a broken item's skill (plus any socketed stat lines) stops working entirely
until it's fixed. It shows a red "BROKEN" line in its tooltip. Repair it at the Combine Forge
before your next fight.

**The Combine Forge (`/ascenditem`)** has six modes:

- **Socket** — put an Enchantment Stone into an open slot.
- **Unlock** — spend a Socket Stone to unlock another slot (up to 5 total).
- **Level** — sacrifice a spare stone to level up a kept one.
- **Remove** — pull a socketed stone back out.
- **Repair** — fix a broken item (costs one of that item's own recipe materials).
- **Refine** — spend 1 of the item's own crafting-recipe materials to push its Refine level up by
  one, from 0 to a cap of **12**. Refine 1 through 6 always succeed; from there the odds drop
  with every step (90% for 6→7, down to 10% for the final 11→12 push) — **the material is spent
  whether the roll lands or not.** Refine 6 and Refine 12 unlock a stronger tier of the item's
  own skill (see each item's "Refine 6 / Refine 12" line below). Every single Refine step also
  adds a small permanent boost while the item is equipped:
  - Melee weapons (swords/axes): **+0.1 Attack Damage per Refine.**
  - Bows/crossbows: a scaling Movement Speed bonus, reaching **+20% at Refine 12.**
  - Armor pieces: **+0.1 Armor per Refine**, each piece independently.
  - Elytra is excluded from this generic bonus.

  A refined item shows it right in the name — e.g. **"+6 Dark Blade"** — and once that prefix
  means something, the item can no longer be renamed at a vanilla anvil.

---

## 6. Elytra (`/elytragear`)

Craft an elytra skin and you're picking a real archetype, not a cosmetic reskin:

- **Speedster** — extra movement speed, softer fall landings.
- **Tank** — more armor.
- **Glass Cannon** — more attack power, at the cost of defense.

`/elytragear` gives you a quick-swap loadout: stow your elytra and re-equip your chestplate (or
the reverse) without digging through your inventory, plus offhand firework rockets that
auto-refill from the loadout while gliding. Sneak + left-click the ground to instantly equip the
stored elytra.

---

## 7. Deviants (`/deviant`)

`/deviant buy` gets you a Deviant skull for **64 Emeralds** — right-click it to summon or recall
your companion, shift-right-click to manage it. A Deviant is **not a mount** — it follows you,
breaks off to fight whatever you're fighting, and returns once the fight's over. Gear it up in
`/deviant equip` with its own armor and weapon, exactly like gearing yourself — that's what makes
it stronger, not a level bar. `/deviantduel <player>` sets up a straight 1v1 between your Deviant
and someone else's, wherever you're both standing (`/deviantduelaccept` to accept a challenge).

---

## 8. Boss Guide

Every World Boss has its own kit — telegraphed attacks you can see coming and dodge, and
periodic "mass seize" ultimates that mark everyone nearby at once. **Every boss also has a
second, alternate "variant" kit that can spawn in its place** — same boss, same stats, a
completely different set of skills — so a given fight could be either kit. Ordinary melee/ranged
attacks from a boss are still normal vanilla-style hits (dodgeable by positioning); only the
named skills below are scripted and always land if their condition is met.

### Piglin Brute (base kit)
A melee/lava brawler. Also periodically buffs its own speed and resistance for a few seconds
while closing in — not something you can play around, just a sign it's about to hit harder for a
moment.

- **Magma Cleave** *(18s cooldown)* — Fires a locked-in flame/lava line 20 blocks in a fixed
  direction: melee damage, knockback, 2s fire. Telegraph: 1.2s growing flame/lava line while the
  boss stands frozen. Dodge: step off the line before it fires.
- **Ground Slam Shockwave** *(45s cooldown)* — A 5-block-radius AoE slam: melee damage and strong
  knockback to anyone still inside. Telegraph: 2s growing dust ring. Dodge: leave the ring before
  it resolves.
- **Molten Rage** *(100s cooldown)* — A 6s channel that marks 3 nearby players/Deviants with a
  lava-crack tile; anyone still on their tile when it ends takes damage, 5s fire, and the boss
  gains a 20s Strength II buff. Telegraph: channel % on the action bar plus a personal warning
  per tile. Counter: move off your tile before it resolves, or deal 15% of the boss's max HP
  during the channel to interrupt it outright.
- **Molten Manacle** *(80s cooldown)* — Shackles everyone within 100 blocks; after a windup,
  anchors and hits each target 4 times with lava eruptions plus fire. Telegraph: "You are
  shackled!" warning, 1.5s windup. Counter: players can escape by getting more than 10 blocks
  away during the windup — Deviants can't.

### Piglin Brute — Bloodgorger (variant kit)
An executioner/berserker theme instead of the lava-hazard base kit. Also buffs its own Strength
and Speed the lower its HP drops.

- **Executioner's Oath** *(100s cooldown)* — Marks the lowest-HP nearby player/Deviant, channels
  for 4s; if the mark is still in range when it resolves, they take an unblockable heavy hit
  (~1.8x normal melee) and the boss gains a 10s Strength II buff. Telegraph: crimson crit
  particles on the marked target for the full channel. Dodge: get outside 30 blocks before it
  ends.
- **Chain Shackles** *(75s cooldown)* — Hooks up to 3 players/Deviants within 40 blocks, yanks
  them into a tight cluster near the boss after a 1s windup; anyone still clustered after a short
  pause takes an AoE burst. Counter: sprint away from the cluster point during the brief pause.
- **Warstomp Eruption** *(42s cooldown)* — A 5-block-radius AoE slam: damage plus 5s Weakness II
  to anyone still inside. Telegraph: 2s growing dust ring. Dodge: leave the ring before it
  resolves.
- **Piercing Javelin** *(16s cooldown)* — Fires a locked-in spear line 20 blocks in a fixed
  direction that pierces everyone in its path: damage plus 2s Slowness II each. Telegraph: 1s
  growing line while frozen. Dodge: step off the line before it fires.

### Zombified Piglin (base kit)
A swarm/chaos fighter leaning on real decoy mobs instead of a solo AoE fight. Also periodically
buffs its own speed and fire resistance while closing in.

- **Golden Horde** *(55s cooldown)* — Spawns 4 mobile clones that chain-detonate: killing one
  primes it for a 0.4s fuse before it explodes, which can chain-prime nearby clones; any survivor
  auto-detonates after 10s. Counter: spread your kills out so one detonation can't chain into the
  next.
- **Soul Brand** *(20s cooldown)* — Brands a random nearby player with a 5s tether; if it holds,
  it detonates for damage and knockback. Telegraph: a visible gold tether line from boss to
  target. Dodge: get more than 8 blocks away or break line of sight before it fires.
- **Chaos Rush** *(90s cooldown)* — Three blinks (~0.7s apart) to points near the target, each
  leaving a scorch patch that lingers 3s — the final blink's patch hits immediately. Dodge: watch
  where each blink lands, stay out of the fire rings, especially the last one.
- **Feeding Frenzy** *(85s cooldown)* — Marks everyone in range, surrounds each target with 3
  decoy Revelers during a 1.5s windup; if it holds, anchors and hits the target 4 times with
  slows. Counter: one of the 3 decoys is a hidden "anchor" — killing it before the windup ends
  breaks the grapple for that target.

### Zombified Piglin — Plaguemaw (variant kit)
A decay/plague theme instead of the base kit's chaos/gold-swarm one. Also pulses a short Wither
aura on anyone within melee range.

- **Grave Contract** *(90s cooldown)* — A 4s channel that curses up to 3 nearby players/Deviants;
  unbroken curses detonate for heavy damage plus 3s Wither II when the channel ends. Telegraph:
  soul particles gathering over each cursed target. Dodge: get outside 30 blocks before it
  resolves.
- **Chain of Greed** *(55s cooldown)* — Golden chains bind up to 3 players/Deviants within 40
  blocks, pull them into a cluster near the boss after a 1s windup, followed by an AoE burst.
  Counter: sprint away from the cluster point during the brief pause.
- **Rotting Legion** *(20s cooldown)* — Raises 3 weak, non-persistent zombie adds that burst into
  a poison cloud when killed — anything still standing after 8s quietly rots away instead.
  Counter: kill the adds at range so their poison burst doesn't catch you.
- **Plague Volley** *(18s cooldown)* — A 1s telegraph, then a spread of rotten gold ingots that
  shatter into lingering poison clouds on impact. Dodge: watch each impact ring and stay out of
  the clouds after they land.

### Ravager (base kit)
A trample/charge/quake fighter — earthy particles and roars instead of Piglin Brute's fire theme.
Also buffs its own speed and strength while closing in.

- **Bull Rush** *(15s cooldown)* — A 12-block stepped charge after a windup; the first entity it
  clips takes damage, knockback, and Nausea II. Telegraph: 0.9s of pawing the ground with dust.
  Dodge: side-step the charge line.
- **Quaking Stomp** *(38s cooldown)* — A 6-block-radius AoE slam: damage, strong knockback,
  Slowness III, and Darkness — automatically chains into Quake Fireballs. Telegraph: 2s growing
  dust ring. Dodge: leave the ring before impact.
- **Quake Fireballs** *(auto-chained off Quaking Stomp)* — 5 lobbed fireballs arc toward players
  within 20 blocks and explode on impact, igniting the ground. Dodge: track the flame/smoke
  trails, move off the impact spot.
- **Warhorn Bellow** *(75s cooldown)* — A 5s freeze during which the boss reflects damage, then
  launches a boulder at every player/Deviant within 20 blocks: damage, Slowness III, knockback.
  Counter: burst it down during the 5s freeze punish window; dodge the boulders after by breaking
  line of sight or moving.
- **Tectonic Impale** *(78s cooldown)* — Marks everyone within 100 blocks with a 1s growing crack
  underfoot; unbroken marks get anchored and hit 3 times with Slowness IV. Telegraph: "the ground
  cracks beneath you — JUMP!" Counter: jumping right as the crack resolves fully avoids it for
  players — Deviants can't.

### Ravager — Thornback (variant kit)
A nature/entangle theme instead of the base kit's earth/quake one. Also gains stacking
Resistance the longer it stays roughly still, resetting the moment it moves.

- **Bramble Fury** *(78s cooldown)* — A 4.5s channel that roots the ground under up to 3 nearby
  players/Deviants; anyone still standing there when it ends takes heavy damage plus 3s Slowness
  IV. Telegraph: a ring of thorns growing at each marked player's feet. Dodge: move off your
  marked spot before the channel ends.
- **Thorned Snare** *(45s cooldown)* — Roots up to 3 players/Deviants within 40 blocks in place
  for 1.5s, then bursts around each of them. Not escapable once rooted — unlike most of this
  boss's other seize skills, there's no player-only counter; focus the boss instead.
- **Splintering Charge** *(20s cooldown)* — A 16-block stepped charge after a telegraph that
  damages everyone it clips and leaves thorn patches that slow anyone standing in them for 4s.
  Telegraph: 1.2s of pawing the ground. Dodge: side-step the charge, then stay off the thorn
  patches.
- **Spore Burst** *(15s cooldown)* — A 1s telegraph then a 10-block cone of spore pods: damage
  plus Poison to everyone caught in the cone. Dodge: step outside the cone's arc before it fires.

### Evoker (base kit)
A mage/caster — its normal vanilla spellcasting (vexes, fangs, curing) is fully disabled in favor
of this kit. Also periodically buffs its own speed and absorption while closing in.

- **Arcane Bolt** *(10s cooldown)* — Fires a locked-in particle line 22 blocks in a fixed
  direction: damage plus 2s Weakness II on the first hit. Telegraph: 1s growing particle line
  while frozen. Dodge: step off the line before it fires.
- **Arcane Cataclysm** *(20s cooldown)* — A 12-block-radius AoE burst: damage and strong
  knockback to anyone still inside. Telegraph: 2s growing purple ring. Dodge: leave the ring
  before it resolves.
- **Spectral Conscription** *(20s cooldown)* — Telegraphs 4 spawn points in a line, then summons
  4 real Vindicators there (despawn after 6s) as extra adds. Not dodgeable, but the 1.5s
  telegraph is a heads-up to reposition.
- **Glacial Ascension** *(60s cooldown)* — Marks everyone within 100 blocks (requires line of
  sight); an unbroken mark gets hoisted into the air and hit by 5 icicles while suspended.
  Telegraph: a 5s mark, cancelled by breaking line of sight or leaving range. Counter: hide
  behind terrain until the mark window passes.
- **Ritual of Unmaking** *(20s cooldown)* — A 6s channel that marks 3 nearby players/Deviants
  with a rune; anyone still on it when it ends takes damage, Darkness, and the boss gains a 20s
  Strength II buff. Telegraph: channel % on the action bar plus a personal warning per rune.
  Counter: move off your rune before it resolves, or deal 15% of the boss's max HP during the
  channel to interrupt it.

### Evoker — Voidcaller (variant kit)
A shadow/teleport theme instead of the base kit's ice/arcane one. Also periodically turns briefly
invisible with a burst of speed while repositioning — a deliberate contrast to the base kit,
which avoids invisibility on purpose so its telegraphs stay visible.

- **Rite of Unraveling** *(100s cooldown)* — A 5s channel that opens shrinking void zones under
  up to 3 nearby players/Deviants; anyone still standing in a zone once it fully collapses takes
  heavy damage. Telegraph: a shrinking particle ring at each marked player's feet. Dodge: step
  out before it fully closes.
- **Umbral Tethers** *(70s cooldown)* — Silently tethers up to 3 players/Deviants with line of
  sight within 22 blocks, draining their HP every half-second for 4s and healing the Evoker for
  half of what's drained. Counter: break line of sight or get outside 22 blocks to sever the
  tether early.
- **Collapsing Rift** *(40s cooldown)* — Opens a rift overhead after a telegraph, then implodes:
  pulls everyone within 8 blocks toward its center and deals damage. Telegraph: a swirling purple
  portal forming above the boss for ~2s. Dodge: get outside 8 blocks before it collapses.
- **Void Lance** *(14s cooldown)* — Blinks directly behind its current target, then fires a
  piercing dark bolt in a line from its new position, hitting everyone it passes through.
  Telegraph: the teleport itself, with only a brief beat before the bolt fires. Dodge: move out
  of the new firing line immediately.

### Warden (base kit)
A sonic/vibration fighter. Also periodically buffs its own speed and haste while closing in. Its
normal ranged Sonic Boom attack still fires and is dodged the usual vanilla way (line of sight
and distance).

- **Charge Scream** *(60s cooldown)* — A 5s charge-up that ends in a scream: halves the current
  HP of every player within 4 blocks and heals the Warden by the total HP drained. Telegraph: a
  full 5s "charging... X%" action-bar warning plus growing sculk particles. Dodge: get outside 4
  blocks before it releases.
- **Resonant Grasp** *(90s cooldown)* — Marks everyone within 100 blocks — works through walls,
  no line of sight needed. An unbroken mark gets anchored and hit 3 times with Slowness IV and
  Mining Fatigue III. Telegraph: "You are marked! Go still or it will seize you!" over a 4s
  window. Counter: unlike every other seize skill, the way out is standing still or sneaking, not
  running — players can break it that way, Deviants can't.

### Warden — Hollowshade (variant kit)
A close-quarters theme instead of the base kit's sound/pursuit one. Also gains Speed II and
Haste II while any player/Deviant is within melee range.

- **Deafening Collapse** *(90s cooldown)* — A 3.5s channel that ends in a full-radius shockwave:
  damage plus knockback to everyone within 8 blocks (instead of the base kit's single-target
  grasp). Telegraph: growing sculk particles around the boss for the full channel. Dodge: get
  outside 8 blocks before it resolves.
- **Umbral Snare** *(50s cooldown)* — Reaches out to up to 3 players/Deviants within 26 blocks
  and clouds their senses with Blindness plus Slowness III for 3.5s. Not escapable once it
  lands — keep your distance beforehand.
- **Tremor Pulse** *(35s cooldown)* — 3 expanding damage rings stomped out in quick succession,
  growing out to 9 blocks: damage to anyone caught as each new ring passes over them. Dodge: move
  ahead of or well outside the rings as each one telegraphs outward.
- **Piercing Shriek** *(16s cooldown)* — Fires a locked-in sonic-beam line 20 blocks in a fixed
  direction that pierces everyone in its path. Telegraph: 1.1s growing beam while frozen. Dodge:
  step off the line before it fires.

### Witch (base kit)
A mage/caster. Ordinary potion throws are still its vanilla AI (damage-capped to one hit per
5s), not a scripted skill below. Also periodically buffs its own speed with a brief levitation
burst while repositioning.

- **Baleful Hex** *(60s cooldown)* — Marks everyone in range (requires line of sight); an
  unbroken mark gets anchored and hit 4 times with Slowness IV and Nausea II. Telegraph: "You are
  hexed! Break line of sight or it will seize you!" over a 4s window. Counter: hide behind
  terrain until the mark passes.
- **Lingering Miasma** *(20s cooldown)* — Lobs a vial that leaves a lingering poison cloud (6s,
  3.5-block radius) ticking damage on anyone standing in it. Dodge: watch the arc, don't stand in
  the purple cloud after it lands.
- **Potion Shower** *(20s cooldown)* — 3 waves of 5 potions landing near the Witch's own
  position: damage plus Poison II, mainly punishing anyone fighting her in melee. Counter: back
  off from melee range while this is going off.
- **Noxious Backlash** *(10s cooldown, below 50% HP)* — An instant, no-telegraph shove: pushes
  everyone within 5.5 blocks away and applies Slowness II. Counter: a punish for crowding a
  low-HP Witch in melee — back off once she's under half health.
- **Restorative Draught** *(20s cooldown, below 60% HP)* — Heals the Witch for 5% of her max HP.
  Not dodgeable, but its 1s drinking telegraph signals a heal is about to land — good bait for a
  burst window if you can interrupt or outpace it.

### Witch — Cursemother (variant kit)
A curse/control theme instead of the base kit's potion-throwing one. Also gains stacking
Resistance while any target is currently bound by Wither Bind.

- **Coven's Reckoning** *(60s cooldown)* — A 3.5s channel on its current target; if they're still
  in range when it resolves, they take a heavy hit plus 7s of Weakness III and Slowness II.
  Telegraph: swirling hex particles on the target for the full channel. Dodge: get outside 26
  blocks before it resolves.
- **Wither Bind** *(40s cooldown)* — Roots up to 3 players/Deviants within 26 blocks in place for
  3s and weakens their outgoing damage for the duration; while bound, they also fuel Blood
  Sacrifice below. Not escapable once rooted — burst the Witch down instead.
- **Blood Sacrifice** *(22s cooldown)* — Drains HP from any currently-bound target(s) and heals
  the Witch for most of what's drained. Counter: this only has something to drain while Wither
  Bind is active — breaking line of sight or killing the Witch before a bind lands denies it
  entirely.
- **Cursed Bramble Volley** *(18s cooldown)* — A 1s telegraph then a 12-block cone of hex-thorns:
  damage plus 5s of stacking Poison II. Dodge: step outside the cone's arc before it fires.

---

## 9. Gear Info

Every gear item is crafted at a normal crafting table from a fixed recipe (1x World Boss
Heartstone + materials from two different bosses + 1-3 currency ore), and comes with its own
skill active immediately. `Refine 6` / `Refine 12` lines below show how the item's skill
strengthens once it's Refined to those levels at the Combine Forge (see section 5) — Refine also
separately grants generic flat Attack Damage/Armor/Movement Speed per step, regardless of skill tier.

### Weapons

#### Piglin Brute

- **Dragon Slaying Blade** — *Dragon's Wrath*
  Right-click to unleash a linear slash in front of you.
  - Activate: Right-click, either hand (10s cooldown)
  - Effect: A normal hit (not affected by attack cooldown) + 16 bonus damage to everyone in the slash.
  - Refine 6: x2  |  Refine 12: x3
  - **Recipe:** 1x World Boss Heartstone + 1x Molten Piglin Tusk + 1x Goldrot Marrow + 1x Diamond
- **Composite Bow** — *Piercing Volley*
  Fully-charged shots detonate in a 5-block radius.
  - Activate: Hold the draw for 5s until charged, then release (releasing early is a normal shot; no cooldown — holding is the charge)
  - Effect: 12 damage to every enemy within a 5-block radius.
  - Refine 6: x2  |  Refine 12: x3
  - Plus Bleed: 3/4.5/7.5 dmg per sec for 5s (Refine 0 / 6 / 12).
  - **Recipe:** 1x World Boss Heartstone + 1x Goldrot Marrow + 1x Bonecrusher Hide + 1x Gold Ingot + 1x Lapis Lazuli
- **Dagger** — *Backstab*
  Punishes a target that isn't watching you; every strike quickens your feet.
  - Activate: Speed III (1.5s) on every hit, always; +5 backstab bonus damage only while the target isn't facing you (no cooldown)
  - Effect: +10 bonus damage + 1s Slowness II on a backstab.
  - Refine 6: +20 dmg  |  Refine 12: +30 dmg
  - **Recipe:** 1x World Boss Heartstone + 1x Warpath Sinew + 1x Hexweaver Totem Shard + 1x Diamond + 1x Gold Ingot + 1x Lapis Lazuli
- **Corrupted Sword** — *Voidgrasp*
  Hits can corrupt a foe's body and mind.
  - Activate: On-hit chance proc (no cooldown)
  - Effect: 12% chance to inflict Slowness VI, Wither I, Blindness, and Weakness II for 10s.
  - Refine 6: 12.5s  |  Refine 12: 15s
  - **Recipe:** 1x World Boss Heartstone + 1x Hexweaver Totem Shard + 1x Deep Dark Marrow + 1x Gold Ingot
- **Golden Splitter** — *Splitting Edge*
  A killing blow's leftover force chains to a second foe.
  - Activate: On a killing blow only (no cooldown)
  - Effect: Chains to the nearest enemy for 80% of the kill damage.
  - Refine 6: 110%  |  Refine 12: 140%
  - **Recipe:** 1x World Boss Heartstone + 1x Deep Dark Marrow + 1x Hexbrewer's Bile + 1x Lapis Lazuli + 1x Emerald
- **Royal Slash** — *Sovereign's Edict*
  Marks a foe as fair game for the whole party.
  - Activate: On-hit chance to mark Exposed — the next hit from anyone on that target crits
  - Effect: 12% chance, Exposed lasts 3s.
  - Refine 6: 4s  |  Refine 12: 5s
  - **Recipe:** 1x World Boss Heartstone + 1x Hexbrewer's Bile + 1x Molten Piglin Tusk + 1x Emerald + 1x Iron Ingot + 1x Copper Ingot
- **Daedric** — *Windup Shot*
  A patient draw detonates in a 7-block radius; a rushed one doesn't.
  - Activate: Hold the draw — 50%+ of a 7s charge = bonus tier, a full 7s hold = bigger bonus + brief root on release (releasing under 50% is a normal shot; no cooldown — holding is the charge)
  - Effect: 50%+ held = +24 dmg; fully held = +48 dmg + 5s root, to every enemy within 7 blocks.
  - Refine 6: +48/+96 dmg  |  Refine 12: +72/+144 dmg
  - **Recipe:** 1x World Boss Heartstone + 1x Molten Piglin Tusk + 1x Withered Gold Dust + 1x Iron Ingot
- **Magi Scythe** — *Soul Siphon*
  A mage's reaping blade, drawing both power and life from its victims.
  - Activate: On a fully-charged hit, chance to siphon a buff and life from the target (no cooldown).
  - Effect: 15% chance to grant +4 flat bonus damage for 6s, and heal for 20% of this hit's damage dealt.
  - Refine 6: 20% chance, 30% heal  |  Refine 12: 25% chance, 40% heal
  - **Recipe:** 1x World Boss Heartstone + 1x Withered Gold Dust + 1x Bonecrusher Hide + 1x Copper Ingot + 1x Ender Pearl
- **Watcher Claymore** — *Vigilant Eye*
  It watches back — and answers whoever dares watch first.
  - Activate: Passive — bonus damage against a mob currently targeting you (no cooldown, no chance).
  - Effect: +20 bonus damage vs. a mob whose AI target is you (mobs only — players never trigger it).
  - Refine 6: +30  |  Refine 12: +40
  - **Recipe:** 1x World Boss Heartstone + 1x Bonecrusher Hide + 1x Illusory Ichor + 1x Ender Pearl + 1x Diamond + 1x Gold Ingot

#### Zombified Piglin

- **Netherite Crossbow** — *Molten Bolt*
  Bolts explode in a fire burst on impact.
  - Activate: Any hit or landing while off cooldown (6s cooldown)
  - Effect: 18 fire damage, 6-block radius, knockup/knockback, ignites for 10s.
  - Refine 6: x2  |  Refine 12: x3
  - **Recipe:** 1x World Boss Heartstone + 1x Illusory Ichor + 1x Deep Dark Marrow + 1x Diamond
- **Bloodthirst** — *Blood Drain*
  A starving edge that drinks its foe's blood to mend its wielder's wounds.
  - Activate: Right-click to enter Blood Drain for 30s (cooldown 30s once it ends). Every hit while active applies a 10s bleed on the target (refreshed, not stacked, by later hits).
  - Effect: bleed deals 10% of that hit's damage over its 10s duration. Each bleed tick heals you back for the same amount, as long as you're still wielding this weapon.
  - Refine 6: 20% of hit damage  |  Refine 12: 30% of hit damage
  - **Recipe:** 1x World Boss Heartstone + 1x Cauldron-Bound Essence + 1x Molten Piglin Tusk + 1x Lapis Lazuli + 1x Emerald + 1x Iron Ingot
- **Yamato** — *Perfect Cut*
  A planted, patient stance cuts deepest.
  - Activate: Bonus scales with time stood still before swinging, capped at 5s (no cooldown)
  - Effect: +10 true damage per 0.5s still, capped at +100.
  - Refine 6: cap +160  |  Refine 12: cap +220
  - A fully-charged strike cannot be evaded. While charging or fully charged: immune to knockback — only moving on your own breaks the stance.
  - **Recipe:** 1x World Boss Heartstone + 1x Molten Piglin Tusk + 1x Goldrot Marrow + 1x Emerald
- **Yamato Thunder Sword** — *Stormcaller*
  A blade that calls down the storm — fiercer still when the sky itself answers.
  - Activate: On a fully-charged hit, chance to strike the target with real lightning (no cooldown).
  - Effect: 20% chance to strike the target with lightning, dealing +15 bonus damage and Slowness II (2s). While it's thundering in your world: guaranteed, +50% bonus damage, and a second bolt chains to the nearest enemy within 4 blocks.
  - Refine 6: 25% chance, +30 bonus damage  |  Refine 12: 30% chance, +45 bonus damage
  - **Recipe:** 1x World Boss Heartstone + 1x Goldrot Marrow + 1x Bonecrusher Hide + 1x Iron Ingot + 1x Copper Ingot
- **Brimstone Claymore** — *Molten Wake*
  Every third swing leaves the ground itself burning.
  - Activate: Every 3rd hit erupts a lingering fire zone at the target's feet (4s, no cooldown on the trigger).
  - Effect: The zone ticks 8 fire damage per second to anyone standing in it (radius 2).
  - Refine 6: 12/tick  |  Refine 12: 16/tick
  - **Recipe:** 1x World Boss Heartstone + 1x Bonecrusher Hide + 1x Hexweaver Totem Shard + 1x Copper Ingot + 1x Ender Pearl + 1x Diamond
- **Ember Blade** — *Simmer*
  Keep swinging and the blade keeps getting hotter.
  - Activate: Passive — consecutive hits build Simmer stacks (max 5, no cooldown). Taking damage cools the blade, clearing all stacks.
  - Effect: +10 bonus fire damage per Simmer stack, max 5 stacks (50 at cap).
  - Refine 6: +20/stack (100 at cap)  |  Refine 12: +30/stack (150 at cap)
  - **Recipe:** 1x World Boss Heartstone + 1x Hexweaver Totem Shard + 1x Direwail Sculk Node + 1x Ender Pearl
- **Soul Stealer** — *Wraith's Mark*
  A stolen soul arms the very next strike.
  - Activate: On killing blow, arms your next hit with bonus true damage and lifesteal (no cooldown).
  - Effect: +50 bonus true damage on the next hit after a kill, plus 100% lifesteal on that hit.
  - Refine 6: +100  |  Refine 12: +150
  - **Recipe:** 1x World Boss Heartstone + 1x Direwail Sculk Node + 1x Hexbrewer's Bile + 1x Diamond + 1x Gold Ingot
- **Whisperwind** — *Silent Gale*
  A blade that moves as quietly as the wind itself.
  - Activate: Passive — bonus damage and lifesteal while sneaking (no cooldown, no chance).
  - Effect: +3 bonus damage and 10% lifesteal while sneaking.
  - Refine 6: +6  |  Refine 12: +10
  - **Recipe:** 1x World Boss Heartstone + 1x Hexbrewer's Bile + 1x Cindermaw Scale + 1x Gold Ingot + 1x Lapis Lazuli + 1x Emerald

#### Warden

- **Mythic Blade** — *Arcane Surge*
  Walk for 3s to charge a true-damage strike.
  - Activate: Charge while walking (not sneaking) for 3s, then land a hit. No cooldown.
  - Effect: 20 true damage + knockback.
  - Refine 6: x2  |  Refine 12: x3
  - Guaranteed minimum: 30 damage.
  - **Recipe:** 1x World Boss Heartstone + 1x Illusory Ichor + 1x Direwail Sculk Node + 1x Lapis Lazuli + 1x Emerald + 1x Iron Ingot
- **Creation Splitter** — *Reality Cleave*
  Right-click to unleash a wide half-moon slash in front of you.
  - Activate: Right-click, either hand (10s cooldown)
  - Effect: A normal hit (not affected by attack cooldown) + 16 bonus damage to everyone in the slash.
  - Refine 6: x2  |  Refine 12: x3
  - **Recipe:** 1x World Boss Heartstone + 1x Direwail Sculk Node + 1x Cauldron-Bound Essence + 1x Emerald
- **Bloodvein** — *Rupture*
  Hits open a stacking bleeding wound.
  - Activate: Any hit (no cooldown)
  - Effect: 3 bleed dmg/tick per stack, 1s ticks for 4s, max 4 stacks.
  - Refine 6: x2 tick damage  |  Refine 12: x3
  - **Recipe:** 1x World Boss Heartstone + 1x Cindermaw Scale + 1x Goldrot Marrow + 1x Copper Ingot + 1x Ender Pearl + 1x Diamond
- **Demonic Greatsword** — *Vengeance*
  The closer to death, the harder it hits back.
  - Activate: Passive — scales with your own missing HP% (no cooldown)
  - Effect: +0.6 bonus damage per 1% missing HP, caps at 75% missing.
  - Refine 6: x2 rate  |  Refine 12: x3 rate
  - **Recipe:** 1x World Boss Heartstone + 1x Goldrot Marrow + 1x Splintered Horn + 1x Ender Pearl
- **Demonic Sword** — *Fell Presence*
  Hits can blind a foe while the sword feeds on their pain.
  - Activate: On-hit chance proc (no cooldown)
  - Effect: 15% chance to Blind the target for 5s, dealing 4 bonus damage and healing you 1.
  - Refine 6: 6 dmg / 2 heal  |  Refine 12: 8 dmg / 3 heal
  - **Recipe:** 1x World Boss Heartstone + 1x Splintered Horn + 1x Hexweaver Totem Shard + 1x Diamond + 1x Gold Ingot
- **Frostmorne** — *Soulreap*
  A blade that hungers for the fallen, growing colder with every soul it claims.
  - Activate: On killing blow, permanently gain a Soul stack (max 15). Resets on death or when you switch away from this weapon (no cooldown).
  - Effect: +3 bonus damage per Soul stack, max 15 stacks. At max stacks, nearby enemies (radius 3) are slowed (Slowness II).
  - Refine 6: +6 bonus damage/stack  |  Refine 12: +9 bonus damage/stack
  - **Recipe:** 1x World Boss Heartstone + 1x Resonant Echo Shard + 1x Hexbrewer's Bile + 1x Lapis Lazuli
- **Demigod's Unholy Blade** — *Unholy Execution*
  The lower they fall, the harder this blade finishes them.
  - Activate: Passive — bonus damage scales up the lower the target's HP is (no cooldown, no chance).
  - Effect: +17 bonus true damage, scaling from 0 at 50% target HP to full value at 20% HP and below.
  - Refine 6: +24 full-scale  |  Refine 12: +30 full-scale
  - **Recipe:** 1x World Boss Heartstone + 1x Bloodforged Gold Shard + 1x Withered Gold Dust + 1x Iron Ingot + 1x Copper Ingot + 1x Ender Pearl
- **Fallen God Spear** — *Skyfall Pin*
  A relic of a god that fell — it still remembers how to pin things down.
  - Activate: On a fully-charged hit, chance to root the target in place (no cooldown).
  - Effect: 20% chance to root the target (Slowness VI, movement negated) for 1.5s, dealing +20 bonus damage.
  - Refine 6: 25% chance, +40 bonus damage  |  Refine 12: 30% chance, +60 bonus damage
  - **Recipe:** 1x World Boss Heartstone + 1x Withered Gold Dust + 1x Warpath Sinew + 1x Copper Ingot
- **Legendary Sword** — *Legend's Might*
  No gimmick. No condition. Just a legendary edge.
  - Activate: Passive — flat bonus damage on every hit, no gate at all.
  - Effect: +10 bonus damage, every hit.
  - Refine 6: +15  |  Refine 12: +20
  - **Recipe:** 1x World Boss Heartstone + 1x Illusory Ichor + 1x Deep Dark Marrow + 1x Diamond + 1x Gold Ingot + 1x Lapis Lazuli
- **True Creation Splitter** — *Genesis Cut*
  A single cut, given the chance to happen twice — and spread.
  - Activate: On a fully-charged hit, chance to deal an instant second hit that also cleaves outward (no cooldown).
  - Effect: 12% chance to deal a second hit (50% of the original) on the same target, and cleave that amount to every other hostile enemy within 5 blocks.
  - Refine 6: 16% chance  |  Refine 12: 20% chance
  - **Recipe:** 1x World Boss Heartstone + 1x Deep Dark Marrow + 1x Cauldron-Bound Essence + 1x Gold Ingot

#### Witch

- **Dark Blade** — *Umbral Reap*
  Right-click to shroud yourself in dark power for 10s — every full swing adds damage + Blindness + Slowness.
  - Activate: Right-click, either hand (20s cooldown after the buff ends)
  - Effect: Full swings (not on attack cooldown) add 16 damage + 2s Blindness + Slowness VI (1s) while active.
  - Refine 6: x2  |  Refine 12: x3
  - **Recipe:** 1x World Boss Heartstone + 1x Cauldron-Bound Essence + 1x Molten Piglin Tusk + 1x Emerald
- **Toxic Long Sword** — *Venom Cascade*
  Strike the ground to summon a lingering poison cloud.
  - Activate: Hit the ground, left-click a block (20s cooldown)
  - Effect: 4 damage per tick to hostile monsters, 15-block radius, lingers 10s. Enemy players count as hostile too, but only while the server's real PVP setting is on.
  - Refine 6: x2  |  Refine 12: x3
  - **Recipe:** 1x World Boss Heartstone + 1x Molten Piglin Tusk + 1x Goldrot Marrow + 1x Iron Ingot + 1x Copper Ingot
- **Thundering Pulse** — *Thunderclap Arrow*
  Fully-charged shots detonate in an 8-block AoE burst.
  - Activate: Hold the draw for 8s until charged, then release (releasing early is a normal shot; no cooldown — holding is the charge)
  - Effect: 45 damage, 8-block radius, plus a real lightning strike on every enemy hit for another 45 damage, plus a 3s root + 3s Blindness.
  - Refine 6: x2  |  Refine 12: x3
  - **Recipe:** 1x World Boss Heartstone + 1x Goldrot Marrow + 1x Bonecrusher Hide + 1x Copper Ingot + 1x Ender Pearl + 1x Diamond
- **Riesling Crossbow** — *Frost Lock*
  A blast on impact slows, fatigues, and damages everyone nearby.
  - Activate: Any hit or landing while off cooldown (7s cooldown)
  - Effect: 30 damage + Slowness II + Mining Fatigue I (2s), to everyone within a 7-block radius.
  - Refine 6: x2 damage & duration  |  Refine 12: x3
  - **Recipe:** 1x World Boss Heartstone + 1x Bonecrusher Hide + 1x Hexweaver Totem Shard + 1x Ender Pearl
- **Corrupted Greatsword** — *Corruptor's Toll*
  Rewards opening a fight hard, not finishing it.
  - Activate: Any hit (no cooldown; scales off the target's current HP%)
  - Effect: Up to +12 bonus damage at 100% target HP, tapering to 0 below 30% HP.
  - Refine 6: cap +24  |  Refine 12: cap +36
  - **Recipe:** 1x World Boss Heartstone + 1x Bloodforged Gold Shard + 1x Goldrot Marrow + 1x Lapis Lazuli + 1x Emerald
- **Death Bringer** — *Reaper's Toll*
  Spend your own blood to empower the next strike.
  - Activate: Right-click, spends 10% of your current HP (10s lockout)
  - Effect: Full swings (not on attack cooldown) add +3.2 bonus damage per 1% HP spent.
  - Refine 6: x1.2 ratio  |  Refine 12: x1.6 ratio
  - **Recipe:** 1x World Boss Heartstone + 1x Goldrot Marrow + 1x Warpath Sinew + 1x Emerald + 1x Iron Ingot + 1x Copper Ingot
- **Murasama** — *Blood Frenzy*
  A cursed katana that thirsts for blood — sate it, or bleed for it.
  - Activate: Right-click to enter Blood Frenzy for 8s (cooldown 18s once it ends). Each landed hit costs 3% of your current HP and grants a Frenzy stack (max 5, 2s independent expiry).
  - Effect: +15 bonus damage per Frenzy stack, max 5 stacks (75 at cap). Requires a fully-charged swing.
  - Refine 6: +30/stack (150 at cap)  |  Refine 12: +45/stack (225 at cap)
  - **Recipe:** 1x World Boss Heartstone + 1x Warpath Sinew + 1x Hexweaver Totem Shard + 1x Iron Ingot
- **Awakened Lichblade** — *Withering Curse*
  A lich's curse doesn't just hurt — it shrinks what you are.
  - Activate: On hit, applies a Curse mark on the target (4s duration, 10s cooldown, no chance).
  - Effect: -8% of the target's Max Health while cursed.
  - Refine 6: -12% Max Health  |  Refine 12: -16% Max Health
  - **Recipe:** 1x World Boss Heartstone + 1x Hexweaver Totem Shard + 1x Deep Dark Marrow + 1x Copper Ingot + 1x Ender Pearl
- **Hero Sword** — *Heroic Surge*
  Every fallen foe fuels the next swing.
  - Activate: On killing blow, empowers you with bonus damage for 8s, refreshed by subsequent kills (no cooldown).
  - Effect: +20 bonus damage for 8s after a killing blow.
  - Refine 6: +30  |  Refine 12: +40
  - **Recipe:** 1x World Boss Heartstone + 1x Hexbrewer's Bile + 1x Molten Piglin Tusk + 1x Diamond
- **Holy Moonlight Sword** — *Lunar Ward*
  Its edge only truly gleams under moonlight.
  - Activate: Passive — bonus damage while it's night in your world (no cooldown, no chance).
  - Effect: +20 bonus damage at night.
  - Refine 6: +30  |  Refine 12: +40
  - **Recipe:** 1x World Boss Heartstone + 1x Molten Piglin Tusk + 1x Withered Gold Dust + 1x Gold Ingot + 1x Lapis Lazuli
- **Soul Render** — *Rend the Soul*
  Every strike tears a little more of the soul loose.
  - Activate: Every hit applies/refreshes your own Soul Rend mark on the target (max 3 stacks, 4s each, no cooldown).
  - Effect: +15 bonus true damage per Soul Rend stack (max 3 = +45), plus Wither I (4s) per hit while stacked.
  - Refine 6: +20/stack (60 max)  |  Refine 12: +25/stack (75 max)
  - **Recipe:** 1x World Boss Heartstone + 1x Withered Gold Dust + 1x Bonecrusher Hide + 1x Lapis Lazuli + 1x Emerald + 1x Iron Ingot
- **Waxweaver** — *Bound in Wax*
  Each strike winds another thread of wax around its victim.
  - Activate: Every hit stacks a Waxbound mark on the target (max 3, 4s each, no cooldown).
  - Effect: Weakness (amplifier = stack count), plus +10 bonus damage per stack.
  - Refine 6: +15/stack  |  Refine 12: +20/stack
  - **Recipe:** 1x World Boss Heartstone + 1x Bonecrusher Hide + 1x Illusory Ichor + 1x Emerald

#### Ravager

- **Assassination** — *Mark for Death*
  A pulse in a 6-block radius marks enemies; marked enemies detonate for bonus damage once the mark's particle effect fades.
  - Activate: Hold the draw for 6s until charged, then release (releasing early is a normal shot; no cooldown — holding is the charge)
  - Effect: marks every enemy within 6 blocks; 5s later, every enemy still marked takes 36 bonus execute damage.
  - Refine 6: x2  |  Refine 12: x3
  - **Recipe:** 1x World Boss Heartstone + 1x Molten Piglin Tusk + 1x Withered Gold Dust + 1x Lapis Lazuli
- **Creation Weaver** — *Life Weave*
  Walk for 3s to charge a life-giving strike.
  - Activate: Charge while walking (not sneaking) for 3s, then land a hit. No cooldown.
  - Effect: +8 damage + Regeneration I (10s) on hit.
  - Refine 6: x2  |  Refine 12: x3
  - Guaranteed minimum: 30 bonus damage.
  - **Recipe:** 1x World Boss Heartstone + 1x Withered Gold Dust + 1x Bonecrusher Hide + 1x Emerald + 1x Iron Ingot
- **Dante's Devilsword** — *Devil Trigger*
  Bank hits in a short window, then unleash them all at once.
  - Activate: Right-click opens a 30s window (20s cooldown after it closes). Only full swings bank a stack. Released as a 10-block burst hitting every hostile monster in range (enemy players too, but only while the server's real PVP setting is on).
  - Effect: deals the sum of every banked hit's own damage, all at once, on window close (no bonus damage added).
  - A visible aura surrounds you for the whole window — enemies can see it charging.
  - **Recipe:** 1x World Boss Heartstone + 1x Bonecrusher Hide + 1x Illusory Ichor + 1x Iron Ingot + 1x Copper Ingot + 1x Ender Pearl
- **Awakened Devilsword** — *Rebellion Surge*
  Every 4th strike echoes with a free phantom hit.
  - Activate: Passive — every 4th consecutive hit (no cooldown)
  - Effect: A phantom repeat-hit for 100% of the triggering hit's damage, 0.5s later.
  - Refine 6: 120%  |  Refine 12: 140%
  - **Recipe:** 1x World Boss Heartstone + 1x Illusory Ichor + 1x Direwail Sculk Node + 1x Copper Ingot
- **Wolf's Gravestone** — *Pack Alpha*
  Every landed hit quickens your stride.
  - Activate: Any hit within the last 4s adds a speed stack (max 3, no cooldown)
  - Effect: Speed I at 1 stack, Speed II at 2 stacks, Speed III at 3 stacks, 4s duration refreshed per hit.
  - Refine 6: +1s base duration
  - **Recipe:** 1x World Boss Heartstone + 1x Direwail Sculk Node + 1x Cauldron-Bound Essence + 1x Ender Pearl + 1x Diamond
- **Thorned Blade** — *Bramble Ward*
  Thorns wrap the blade — every strike against its wielder is answered in kind.
  - Activate: Passive. 50% chance to retaliate whenever you take melee damage (no cooldown).
  - Effect: Reflects +10 bonus damage and Poison I for 3s back at the attacker.
  - Refine 6: +15 dmg, 4s Poison  |  Refine 12: +30 dmg, 5s Poison
  - **Recipe:** 1x World Boss Heartstone + 1x Cauldron-Bound Essence + 1x Cindermaw Scale + 1x Diamond + 1x Gold Ingot + 1x Lapis Lazuli
- **Bloody Death** — *Death's Harvest*
  A killing blow doesn't end the feast — it starts it.
  - Activate: On killing blow, unleash a life-drain nova (no cooldown — gated by landing a kill).
  - Effect: Drains 15% of each nearby enemy's (radius 5) current HP as bonus true damage, healing you for the total drained.
  - Refine 6: 20% drained  |  Refine 12: 25% drained
  - **Recipe:** 1x World Boss Heartstone + 1x Cindermaw Scale + 1x Goldrot Marrow + 1x Gold Ingot
- **Edge of the Astral Plane** — *Void Rend*
  A wound from beyond the stars refuses to close.
  - Activate: On a fully-charged hit, chance to open a void wound that ticks bonus damage over time (no cooldown).
  - Effect: 20% chance to inflict a Bleed-like wound dealing 6 damage/sec for 4s (2 ticks/sec, non-stacking).
  - Refine 6: 25% chance, 9 dmg/sec  |  Refine 12: 30% chance, 12 dmg/sec
  - **Recipe:** 1x World Boss Heartstone + 1x Goldrot Marrow + 1x Splintered Horn + 1x Lapis Lazuli + 1x Emerald
- **Stormbringer** — *Gale Cutter*
  A gale rides every swing of this blade.
  - Activate: On a fully-charged hit, chance to unleash a gust (no cooldown).
  - Effect: 20% chance to knock up all enemies within radius 3 of the target, dealing +10 bonus damage.
  - Refine 6: 25% chance, +20 bonus damage  |  Refine 12: 30% chance, +30 bonus damage
  - **Recipe:** 1x World Boss Heartstone + 1x Splintered Horn + 1x Hexweaver Totem Shard + 1x Emerald + 1x Iron Ingot + 1x Copper Ingot
- **Storm's Edge** — *Momentum*
  The faster you move, the harder this blade lands.
  - Activate: Passive — bonus damage scales with how far you've moved since your last hit with this weapon (no cooldown, no chance).
  - Effect: Up to +20 bonus damage, scaling linearly with blocks moved (capped at 10 blocks).
  - Refine 6: up to +40  |  Refine 12: up to +60
  - **Recipe:** 1x World Boss Heartstone + 1x Hexweaver Totem Shard + 1x Resonant Echo Shard + 1x Iron Ingot
- **Thunderbrand** — *Branding Curse*
  A brand that turns a foe's own strength against itself.
  - Activate: On a fully-charged hit, chance to brand the target (no cooldown).
  - Effect: 20% chance to brand for 5s; while branded, 20% of any damage the target deals to anyone is reflected back onto itself.
  - Refine 6: 25% chance  |  Refine 12: 30% chance
  - **Recipe:** 1x World Boss Heartstone + 1x Resonant Echo Shard + 1x Hexbrewer's Bile + 1x Copper Ingot + 1x Ender Pearl
- **Thunderbringer** — *Overcharge*
  Every strike builds a charge that has to go somewhere.
  - Activate: Passive — every hit builds a Charge stack (max 5); at 5 stacks, auto-releases a chain to nearby enemies and resets (no cooldown).
  - Effect: +8 bonus damage per stack; at 5 stacks, chains to up to 3 nearby enemies (radius 4) for the accumulated total.
  - Refine 6: +12/stack  |  Refine 12: +16/stack
  - **Recipe:** 1x World Boss Heartstone + 1x Hexbrewer's Bile + 1x Bloodforged Gold Shard + 1x Ender Pearl + 1x Diamond + 1x Gold Ingot

#### Evoker

- **Cyber Katana** — *Neon Slash*
  Right-click to dash through foes in a neon blur.
  - Activate: Right-click, either hand, dash up to 10 blocks (10s cooldown)
  - Effect: A normal hit (not affected by attack cooldown) + 12 bonus damage to everyone struck along the dash.
  - Refine 6: x2  |  Refine 12: x3
  - **Recipe:** 1x World Boss Heartstone + 1x Bonecrusher Hide + 1x Hexweaver Totem Shard + 1x Diamond + 1x Gold Ingot + 1x Lapis Lazuli
- **Flamatic Katana** — *Cinder Combo*
  Sneak for 8s to build a charge — a charged hit roots both fighters and lands 10 rapid slashes.
  - Activate: Charge while sneaking for 8s (persists after you stop sneaking), then land a hit. No cooldown.
  - Effect: 2 absorption on every hit and on charge; charged hit: 3s root + 10 rapid slashes.
  - Refine 6: x2  |  Refine 12: x3
  - **Recipe:** 1x World Boss Heartstone + 1x Hexweaver Totem Shard + 1x Direwail Sculk Node + 1x Gold Ingot
- **Blue Rose** — *Winter's Embrace*
  Blooms a circle of frost that punishes hostile monsters and soothes everyone else who lingers.
  - Activate: Right-click to summon a 10-block-radius freezing field centered on you, lasting 10s (30s cooldown)
  - Effect: Deals 4 damage per second and applies Slowness II to hostile monsters inside (won't refresh an already-slowed target). Enemy players count as hostile too, but only while the server's real PVP setting is on. Everyone else — including you, while standing in your own field — instead gains Regeneration II for as long as the field remains.
  - Refine 6: 20s cooldown  |  Refine 12: 10s cooldown
  - **Recipe:** 1x World Boss Heartstone + 1x Hexbrewer's Bile + 1x Cindermaw Scale + 1x Emerald + 1x Iron Ingot + 1x Copper Ingot
- **Grim** — *Grim Ledger*
  Marks a debt only you can collect on.
  - Activate: On-hit chance to mark; your own follow-up hit in the window is a guaranteed crit
  - Effect: Mark window 3s.
  - Refine 6: 4s  |  Refine 12: 5s
  - **Recipe:** 1x World Boss Heartstone + 1x Cindermaw Scale + 1x Withered Gold Dust + 1x Iron Ingot
- **Angelic Greatsword** — *Aegis Zeal*
  Every hit wraps you in a growing barrier of light — push it too far and it overloads.
  - Activate: Any hit grants a shield stack (max 3); reaching 3 stacks locks the skill out for 25s
  - Effect: +3 shield per stack, 4s independent expiry each; at 3 stacks, the skill locks out (no new stacks) for 25s.
  - Refine 6: +5/stack  |  Refine 12: +7/stack
  - **Recipe:** 1x World Boss Heartstone + 1x Withered Gold Dust + 1x Splintered Horn + 1x Copper Ingot + 1x Ender Pearl
- **Angelic Sword** — *Radiant Focus*
  Consecutive strikes build toward a guaranteed judgment.
  - Activate: Every hit builds a Focus stack (max 5, 6s expiry each); the 5th stack unleashes a guaranteed Judgment strike
  - Effect: below 5 stacks, +2 dmg per stack. At 5 stacks: guaranteed 3.6x damage + heal 15% of the damage dealt, then resets.
  - Refine 6: +4 dmg/stack, 4.0x + 20% heal  |  Refine 12: +6 dmg/stack, 4.4x + 25% heal
  - **Recipe:** 1x World Boss Heartstone + 1x Splintered Horn + 1x Illusory Ichor + 1x Ender Pearl + 1x Diamond + 1x Gold Ingot
- **Infinity** — *Overflow*
  Every strike spills over into your other gear's cooldowns.
  - Activate: Any hit shaves cooldowns on your other equipped gear skills (no cooldown of its own)
  - Effect: -0.3s off other active cooldowns per hit.
  - Refine 6: -0.5s  |  Refine 12: -0.8s
  - **Recipe:** 1x World Boss Heartstone + 1x Illusory Ichor + 1x Resonant Echo Shard + 1x Diamond
- **Excalibur** — *Smite the Unholy*
  A king's blade, sworn against everything that should not walk.
  - Activate: Passive — bonus damage against Undead-type enemies specifically (no cooldown, no chance).
  - Effect: +40 bonus damage vs. Undead targets (zombies, skeletons, withers, zombified piglins, etc).
  - Refine 6: +60 vs. Undead  |  Refine 12: +80 vs. Undead
  - **Recipe:** 1x World Boss Heartstone + 1x Resonant Echo Shard + 1x Cauldron-Bound Essence + 1x Gold Ingot + 1x Lapis Lazuli
- **Star's Edge** — *Mortal Edge*
  Its edge grows hungrier as its foe's strength fades.
  - Activate: 30% chance on hit — bonus damage scales with the target's missing health (no cooldown).
  - Effect: 30% chance per hit to deal +10 bonus damage at full health, ramping up to +30 bonus damage as the target nears death.
  - Refine 6: up to +45  |  Refine 12: up to +60
  - **Recipe:** 1x World Boss Heartstone + 1x Cauldron-Bound Essence + 1x Bloodforged Gold Shard + 1x Lapis Lazuli + 1x Emerald + 1x Iron Ingot
- **Wick Piercer** — *Piercing Wick*
  A needle-fine edge that finds the gaps in any armor.
  - Activate: Passive — every hit pierces a portion of the target's Armor mitigation (no cooldown, no chance).
  - Effect: Pierces 20% of the target's Armor mitigation on this hit.
  - Refine 6: 30% pierced  |  Refine 12: 40% pierced
  - **Recipe:** 1x World Boss Heartstone + 1x Bloodforged Gold Shard + 1x Goldrot Marrow + 1x Emerald

### Armor

Armor is organized into named sets — wearing all 4 pieces of a set grants an additional set
bonus on top of each piece's own individual skill.

#### Piglin Brute

**Dragonsbane Set**

- **Dragonsbane Helmet** — *Keen Eye* (chance to force a critical strike)
  - Activate: Passive: always active while worn
  - Effect: 5% chance per hit to crit (1.5x damage).
  - Refine 6: x2  |  Refine 12: x3
  - **Recipe:** 1x World Boss Heartstone + 1x Bonecrusher Hide + 1x Hexweaver Totem Shard + 1x Lapis Lazuli + 1x Emerald + 1x Iron Ingot
- **Dragonsbane Chestplate** — *Dragonhide Ward* (periodic absorption shield)
  - Activate: Automatic pulse every 20s while worn
  - Effect: Absorption II pulse every 20s.
  - Refine 6: x2  |  Refine 12: x3
  - **Recipe:** 1x World Boss Heartstone + 1x Hexweaver Totem Shard + 1x Direwail Sculk Node + 1x Emerald
- **Dragonsbane Leggings** — *Steady Footing* (knockback resistance)
  - Activate: Passive: always active while worn
  - Effect: 20% knockback resistance.
  - Refine 6: x2  |  Refine 12: x3
  - **Recipe:** 1x World Boss Heartstone + 1x Direwail Sculk Node + 1x Hexbrewer's Bile + 1x Iron Ingot + 1x Copper Ingot
- **Dragonsbane Boots** — *Hunter's Stride* (speed vs. weak/marked foes)
  - Activate: Passive: always active while worn
  - Effect: Speed I, 4s vs. marked/low-HP targets.
  - Refine 6: x2  |  Refine 12: x3
  - **Recipe:** 1x World Boss Heartstone + 1x Hexbrewer's Bile + 1x Cindermaw Scale + 1x Copper Ingot + 1x Ender Pearl + 1x Diamond
- **4-Piece Set Bonus:** Damage dealt to boss-tier enemies +15% (Refine 6: +30%, Refine 12: +45%). Taking damage from a boss-tier enemy grants Strength I for 8s (Refine 6: 16s, Refine 12: 24s) — 10s internal cooldown.

**Champion Petra Set**

- **Champion Petra Helmet** — *Warlord's Focus* (chance to cancel blast, splash & magic dmg)
  - Activate: Passive — rolled each time blast/splash-potion/magic dmg lands
  - Effect: 40% chance to fully cancel blast, splash-potion, and magic damage.
  - Refine 6: 50% chance  |  Refine 12: 60% chance
  - **Recipe:** 1x World Boss Heartstone + 1x Cindermaw Scale + 1x Withered Gold Dust + 1x Ender Pearl
- **Champion Petra Chestplate** — *Petravein Regen* (regens below half health)
  - Activate: Automatic pulse every 15s while below 50% HP
  - Effect: Regen I every 15s below 50% HP.
  - Refine 6: x2  |  Refine 12: x3
  - **Recipe:** 1x World Boss Heartstone + 1x Withered Gold Dust + 1x Splintered Horn + 1x Diamond + 1x Gold Ingot
- **Champion Petra Leggings** — *Bulwark Stance* (less damage while sneaking)
  - Activate: Triggers on taking damage while sneaking
  - Effect: 20% dmg reduction while sneaking.
  - Refine 6: x2  |  Refine 12: x3
  - Total reduction is capped at 60% max, however stacked.
  - **Recipe:** 1x World Boss Heartstone + 1x Splintered Horn + 1x Illusory Ichor + 1x Gold Ingot + 1x Lapis Lazuli + 1x Emerald
- **Champion Petra Boots** — *Earthshaker Step* (fall-landing shockwave)
  - Activate: Triggers on landing
  - Effect: Fall-landing AoE knockback + 4 dmg.
  - Refine 6: x2  |  Refine 12: x3
  - **Recipe:** 1x World Boss Heartstone + 1x Illusory Ichor + 1x Resonant Echo Shard + 1x Lapis Lazuli
- **4-Piece Set Bonus — Champion's Resolve:** On any kill, gain Absorption: 2 pts/1 heart (Refine 6: 4 pts/2 hearts, Refine 12: 6 pts/3 hearts) for 10s.

#### Zombified Piglin

**Fox Set**

- **Fox Helmet** — *Fox Sense* (pings nearby mobs)
  - Activate: Automatic pulse every 15s while worn
  - Effect: Pings hostile mobs (Glowing) every 15s, 10 blocks.
  - Refine 6: x2  |  Refine 12: x3
  - **Recipe:** 1x World Boss Heartstone + 1x Illusory Ichor + 1x Direwail Sculk Node + 1x Lapis Lazuli
- **Fox Chestplate** — *Nimble Core* (chance to dodge a hit)
  - Activate: Triggers on taking damage (chance-based)
  - Effect: 5% chance to dodge a hit entirely.
  - Refine 6: x2  |  Refine 12: x3
  - **Recipe:** 1x World Boss Heartstone + 1x Direwail Sculk Node + 1x Cauldron-Bound Essence + 1x Emerald + 1x Iron Ingot
- **Fox Leggings** — *Swift Legs* (Speed I burst on taking damage)
  - Activate: Triggers on taking damage (skipped if Speed already active)
  - Effect: Speed I for 4s when hit.
  - Refine 6: 7s  |  Refine 12: 10s
  - **Recipe:** 1x World Boss Heartstone + 1x Cauldron-Bound Essence + 1x Cindermaw Scale + 1x Iron Ingot + 1x Copper Ingot + 1x Ender Pearl
- **Fox Boots** — *Silent Paws* (40% chance to negate fall damage, less aggro)
  - Activate: Triggers on fall damage (40% chance)
  - Effect: 40% chance to negate fall damage, halved mob aggro radius.
  - Refine 6: 50% chance  |  Refine 12: 60% chance
  - **Recipe:** 1x World Boss Heartstone + 1x Cindermaw Scale + 1x Goldrot Marrow + 1x Copper Ingot
- **4-Piece Set Bonus — Fox's Fortune:** 5% chance (Refine 6: 10%, Refine 12: 15%) to fully evade any incoming hit — separate from Nimble Core's own per-piece dodge roll.

**Grimdark Gold Set**

- **Grimdark Gold Helmet** — *Gilded Greed* (chance for bonus gold nuggets on kill)
  - Activate: Triggers on kill (chance-based)
  - Effect: 1% chance for bonus gold nuggets on kill.
  - Refine 6: 2% chance  |  Refine 12: 3% chance
  - **Recipe:** 1x World Boss Heartstone + 1x Goldrot Marrow + 1x Splintered Horn + 1x Ender Pearl + 1x Diamond
- **Grimdark Gold Chestplate** — *Golden Bulwark* (absorption on big hits)
  - Activate: Triggers when hit for 6+ damage (6s cooldown)
  - Effect: Absorption I when hit for 6+ dmg.
  - Refine 6: x2  |  Refine 12: x3
  - **Recipe:** 1x World Boss Heartstone + 1x Splintered Horn + 1x Hexweaver Totem Shard + 1x Diamond + 1x Gold Ingot + 1x Lapis Lazuli
- **Grimdark Gold Leggings** — *Molten Legguards* (chance to ignite attackers)
  - Activate: Passive — rolled each time you're hit
  - Effect: 40% chance to ignite an attacker on being hit.
  - Refine 6: 50% chance  |  Refine 12: 60% chance
  - **Recipe:** 1x World Boss Heartstone + 1x Hexweaver Totem Shard + 1x Resonant Echo Shard + 1x Gold Ingot
- **Grimdark Gold Boots** — *Heavy Steps* (knockback resistance)
  - Activate: Passive: always active while worn
  - Effect: 15% knockback resistance.
  - Refine 6: x2  |  Refine 12: x3
  - **Recipe:** 1x World Boss Heartstone + 1x Resonant Echo Shard + 1x Hexbrewer's Bile + 1x Lapis Lazuli + 1x Emerald
- **4-Piece Set Bonus — Golden Frenzy:** Every 30s: Strength I and Resistance I for 5s (Refine 6: Strength II and Resistance II for 10s, Refine 12: Strength III and Resistance III for 15s).

**Grimdark Dark Set**

- **Grimdark Dark Helmet** — *Iron Will* (chance to resist Blindness & knockback)
  - Activate: Passive — rolled each time Blindness would apply
  - Effect: 40% chance to resist Blindness + 20% knockback resist.
  - Refine 6: 50% chance  |  Refine 12: 60% chance
  - **Recipe:** 1x World Boss Heartstone + 1x Hexbrewer's Bile + 1x Bloodforged Gold Shard + 1x Emerald + 1x Iron Ingot + 1x Copper Ingot
- **Grimdark Dark Chestplate** — *Netherite Core* (flat % damage reduction)
  - Activate: Passive — reduces every hit taken
  - Effect: 10% flat damage reduction on every hit.
  - Refine 6: x2  |  Refine 12: x3
  - Total reduction is capped at 60% max, however stacked.
  - **Recipe:** 1x World Boss Heartstone + 1x Bloodforged Gold Shard + 1x Withered Gold Dust + 1x Iron Ingot
- **Grimdark Dark Leggings** — *Grim Stability* (chance to resist Slowness)
  - Activate: Passive — rolled each time Slowness would apply
  - Effect: 40% chance to resist Slowness.
  - Refine 6: 50% chance  |  Refine 12: 60% chance
  - **Recipe:** 1x World Boss Heartstone + 1x Withered Gold Dust + 1x Warpath Sinew + 1x Copper Ingot + 1x Ender Pearl
- **Grimdark Dark Boots** — *Grounded* (reduced knockback near ledges)
  - Activate: Passive: always active while worn
  - Effect: 25% reduced knockback near ledges.
  - Refine 6: x2  |  Refine 12: x3
  - **Recipe:** 1x World Boss Heartstone + 1x Warpath Sinew + 1x Illusory Ichor + 1x Ender Pearl + 1x Diamond + 1x Gold Ingot
- **4-Piece Set Bonus:** Below 30% HP, every 60s: Resistance I for 5s (Refine 6: Resistance II for 10s, Refine 12: Resistance III for 15s).

#### Warden

**Halo Set**

- **Halo Helmet** — *Halo Sight* (Guardian's Grace: near-death save)
  - Activate: Triggers when dropped to <=20% HP (30s cooldown, chance-based)
  - Effect: 40% chance for Resistance II (5s) + Absorption when critically low HP.
  - Refine 6: 50% chance  |  Refine 12: 60% chance
  - **Recipe:** 1x World Boss Heartstone + 1x Goldrot Marrow + 1x Warpath Sinew + 1x Gold Ingot
- **Halo Chestplate** — *Sanctified Core* (heals you in combat)
  - Activate: Automatic pulse every 10s while in combat
  - Effect: Heals 1 heart every 10s in combat.
  - Refine 6: x2  |  Refine 12: x3
  - **Recipe:** 1x World Boss Heartstone + 1x Warpath Sinew + 1x Hexweaver Totem Shard + 1x Lapis Lazuli + 1x Emerald
- **Halo Leggings** — *Blessed Guard* (chance to resist Poison & Wither)
  - Activate: Passive — rolled each time Poison/Wither would apply
  - Effect: 40% chance to resist Poison and Wither.
  - Refine 6: 50% chance  |  Refine 12: 60% chance
  - **Recipe:** 1x World Boss Heartstone + 1x Hexweaver Totem Shard + 1x Deep Dark Marrow + 1x Emerald + 1x Iron Ingot + 1x Copper Ingot
- **Halo Boots** — *Grace Step* (40% chance to negate fall damage)
  - Activate: Triggers on fall damage (40% chance)
  - Effect: 40% chance to negate fall dmg + Slow Falling while sneak-falling.
  - Refine 6: 50% chance  |  Refine 12: 60% chance
  - **Recipe:** 1x World Boss Heartstone + 1x Deep Dark Marrow + 1x Hexbrewer's Bile + 1x Iron Ingot
- **4-Piece Set Bonus:** Every 20s: Regeneration I for 3s (Refine 6: Regeneration II, Refine 12: Regeneration III) and clears every negative effect on you.

**Ellegaard Set**

- **Ellegaard Helmet** — *Tinker's Sight* (reveals invisible enemies + highlights mobs)
  - Activate: Automatic pulse every 20s while worn
  - Effect: Reveals invisible enemies + highlights nearby mobs every 20s, 8 blocks.
  - Refine 6: x2  |  Refine 12: x3
  - **Recipe:** 1x World Boss Heartstone + 1x Hexbrewer's Bile + 1x Molten Piglin Tusk + 1x Copper Ingot + 1x Ender Pearl
- **Ellegaard Chestplate** — *Reinforced Plating* (absorption when low on health)
  - Activate: Triggers when hit while below 50% HP
  - Effect: Absorption I when hit below 50% HP.
  - Refine 6: x2  |  Refine 12: x3
  - **Recipe:** 1x World Boss Heartstone + 1x Molten Piglin Tusk + 1x Withered Gold Dust + 1x Ender Pearl + 1x Diamond + 1x Gold Ingot
- **Ellegaard Leggings** — *Servo Legs* (Speed II burst on sprint start)
  - Activate: Triggers when you start sprinting (8s cooldown)
  - Effect: Speed II for 3s when you start sprinting.
  - Refine 6: 5s  |  Refine 12: 7s
  - **Recipe:** 1x World Boss Heartstone + 1x Withered Gold Dust + 1x Bonecrusher Hide + 1x Diamond
- **Ellegaard Boots** — *Piston Step* (increased jump height)
  - Activate: Triggers when you jump
  - Effect: +0.3 block jump height.
  - Refine 6: x2  |  Refine 12: x3
  - **Recipe:** 1x World Boss Heartstone + 1x Bonecrusher Hide + 1x Illusory Ichor + 1x Gold Ingot + 1x Lapis Lazuli
- **4-Piece Set Bonus — Engineer's Mark:** Every 10s, marks a random nearby monster (or an enemy player in PvP-enabled worlds). Damaging the marked target deals +3 bonus damage (Refine 6: +6, Refine 12: +9) — the mark expires after 10s.

#### Witch

**Black Ninja Set**

- **Black Ninja Helmet** — *Shadow Veil* (sneaking grants Resistance II)
  - Activate: Triggers when you start sneaking (8s cooldown)
  - Effect: Sneaking grants Resistance II for 3s.
  - Refine 6: x2  |  Refine 12: x3
  - **Recipe:** 1x World Boss Heartstone + 1x Hexweaver Totem Shard + 1x Direwail Sculk Node + 1x Diamond + 1x Gold Ingot
- **Black Ninja Chestplate** — *Umbral Guard* (less damage while sneaking)
  - Activate: Triggers on taking damage while sneaking
  - Effect: 15% dmg reduction while sneaking.
  - Refine 6: x2  |  Refine 12: x3
  - Total reduction is capped at 60% max, however stacked.
  - **Recipe:** 1x World Boss Heartstone + 1x Direwail Sculk Node + 1x Hexbrewer's Bile + 1x Gold Ingot + 1x Lapis Lazuli + 1x Emerald
- **Black Ninja Leggings** — *Silent Step* (sneak-attacks have a chance to crit)
  - Activate: Passive — rolled on every melee hit while sneaking
  - Effect: 40% chance to force a critical hit (1.5x dmg) while sneaking.
  - Refine 6: 50% chance  |  Refine 12: 60% chance
  - **Recipe:** 1x World Boss Heartstone + 1x Hexbrewer's Bile + 1x Cindermaw Scale + 1x Lapis Lazuli
- **Black Ninja Boots** — *Shadow Dash* (speed burst exiting sneak)
  - Activate: Triggers when you stop sneaking (6s cooldown)
  - Effect: Speed II burst on exiting sneak.
  - Refine 6: x2  |  Refine 12: x3
  - **Recipe:** 1x World Boss Heartstone + 1x Cindermaw Scale + 1x Withered Gold Dust + 1x Emerald + 1x Iron Ingot
- **4-Piece Set Bonus — Night's Embrace:** The first hit landed right after a Shadow Veil sneak-triggered stealth window ends deals +6 bonus damage (Refine 6: +12, Refine 12: +18).

**White Ninja Set**

- **White Ninja Helmet** — *Clear Mind* (chance to resist Blindness & Nausea)
  - Activate: Passive — rolled each time Blindness/Nausea would apply
  - Effect: 40% chance to resist Blindness and Nausea.
  - Refine 6: 50% chance  |  Refine 12: 60% chance
  - **Recipe:** 1x World Boss Heartstone + 1x Withered Gold Dust + 1x Splintered Horn + 1x Iron Ingot + 1x Copper Ingot + 1x Ender Pearl
- **White Ninja Chestplate** — *Light Guard* (absorption on heavy hits taken)
  - Activate: Triggers when you take 6+ damage (5s cooldown)
  - Effect: Absorption I when hit for 6+ damage.
  - Refine 6: x2  |  Refine 12: x3
  - **Recipe:** 1x World Boss Heartstone + 1x Splintered Horn + 1x Illusory Ichor + 1x Copper Ingot
- **White Ninja Leggings** — *Swift Steps* (periodic Speed I pulse)
  - Activate: Automatic pulse every 15s while worn
  - Effect: Speed I for 3s every 15s.
  - Refine 6: 6s  |  Refine 12: 9s
  - **Recipe:** 1x World Boss Heartstone + 1x Illusory Ichor + 1x Resonant Echo Shard + 1x Ender Pearl + 1x Diamond
- **White Ninja Boots** — *Featherfall* (40% chance to negate fall damage)
  - Activate: Triggers on fall damage (40% chance)
  - Effect: 40% chance to negate fall damage.
  - Refine 6: 50% chance  |  Refine 12: 60% chance
  - **Recipe:** 1x World Boss Heartstone + 1x Resonant Echo Shard + 1x Cauldron-Bound Essence + 1x Diamond + 1x Gold Ingot + 1x Lapis Lazuli
- **4-Piece Set Bonus:** At night, 8% chance (Refine 6: 16%, Refine 12: 24%) per melee hit to force a 1.5x critical strike.

#### Ravager

**Adamantium Set**

- **Adamantium Helmet** — *Adamant Will* (chance to resist Weakness & Mining Fatigue)
  - Activate: Passive — rolled each time Weakness/Mining Fatigue would apply
  - Effect: 40% chance to resist Weakness and Mining Fatigue.
  - Refine 6: 50% chance  |  Refine 12: 60% chance
  - **Recipe:** 1x World Boss Heartstone + 1x Cindermaw Scale + 1x Withered Gold Dust + 1x Iron Ingot
- **Adamantium Chestplate** — *Adamant Core* (regenerating absorption shield)
  - Activate: Automatic pulse every 10s while worn
  - Effect: Regenerating Absorption pool, 4 pts max.
  - Refine 6: x2  |  Refine 12: x3
  - **Recipe:** 1x World Boss Heartstone + 1x Withered Gold Dust + 1x Splintered Horn + 1x Copper Ingot + 1x Ender Pearl
- **Adamantium Leggings** — *Adamant Legs* (full knockback immunity + thorns reflect)
  - Activate: Passive: always active while worn
  - Effect: Full knockback immunity + 1 reflected damage to melee attackers.
  - Refine 6: x2  |  Refine 12: x3
  - **Recipe:** 1x World Boss Heartstone + 1x Splintered Horn + 1x Illusory Ichor + 1x Ender Pearl + 1x Diamond + 1x Gold Ingot
- **Adamantium Boots** — *Adamant Step* (40% chance to negate stomp damage)
  - Activate: Triggers when attacked (40% chance to negate)
  - Effect: 40% chance to fully negate mob stomp/trample damage.
  - Refine 6: 50% chance  |  Refine 12: 60% chance
  - **Recipe:** 1x World Boss Heartstone + 1x Illusory Ichor + 1x Resonant Echo Shard + 1x Diamond
- **4-Piece Set Bonus:** Every 25s: Resistance I for 4s (Refine 6: Resistance II for 8s, Refine 12: Resistance III for 12s).

**Green Ninja Set**

- **Green Ninja Helmet** — *Poison Ward* (chance to cleanse Poison + grant Regeneration)
  - Activate: Passive — rolled on every Poison damage tick
  - Effect: 40% chance per tick to cure Poison, granting Regeneration for 10s.
  - Refine 6: 50% chance, 15s Regen  |  Refine 12: 60% chance, 20s Regen
  - **Recipe:** 1x World Boss Heartstone + 1x Resonant Echo Shard + 1x Cauldron-Bound Essence + 1x Gold Ingot + 1x Lapis Lazuli
- **Green Ninja Chestplate** — *Verdant Core* (regen in sustained combat)
  - Activate: Automatic pulse every 8s while worn
  - Effect: Regen I after 5+ consecutive combat seconds.
  - Refine 6: x2  |  Refine 12: x3
  - **Recipe:** 1x World Boss Heartstone + 1x Cauldron-Bound Essence + 1x Bloodforged Gold Shard + 1x Lapis Lazuli + 1x Emerald + 1x Iron Ingot
- **Green Ninja Leggings** — *Thicket Legs* (thorns/knockback resist)
  - Activate: Passive: always active while worn
  - Effect: Immune to thorns/cactus/berry dmg + 10% KB resist.
  - Refine 6: x2  |  Refine 12: x3
  - **Recipe:** 1x World Boss Heartstone + 1x Bloodforged Gold Shard + 1x Goldrot Marrow + 1x Emerald
- **Green Ninja Boots** — *Leaf Step* (foliage fall-dodge + landing Strength)
  - Activate: Triggers on every landing; foliage dodge is chance-based
  - Effect: 40% chance to negate fall damage on grass/foliage + Strength I (5s) on any landing.
  - Refine 6: 50% dodge chance  |  Refine 12: 60% dodge chance
  - **Recipe:** 1x World Boss Heartstone + 1x Goldrot Marrow + 1x Warpath Sinew + 1x Iron Ingot + 1x Copper Ingot
- **4-Piece Set Bonus:** Every melee hit (not vs. Deviants) poisons the target: Poison I for 3s (Refine 6: Poison II for 6s, Refine 12: Poison III for 9s).

**Blue Ninja Set**

- **Blue Ninja Helmet** — *Tidal Sight* (water breathing/vision + chance to root on hit)
  - Activate: Water Breathing/Vision while in water; root chance rolled on your melee hits
  - Effect: Water Breathing + Night Vision underwater; 40% chance to Slowness II your target on hit.
  - Refine 6: 50% chance  |  Refine 12: 60% chance
  - **Recipe:** 1x World Boss Heartstone + 1x Warpath Sinew + 1x Hexweaver Totem Shard + 1x Copper Ingot + 1x Ender Pearl + 1x Diamond
- **Blue Ninja Chestplate** — *Frost Core* (chance to slow your attacker)
  - Activate: Triggers on taking damage (chance-based)
  - Effect: 10% chance to Slowness I your attacker.
  - Refine 6: x2  |  Refine 12: x3
  - **Recipe:** 1x World Boss Heartstone + 1x Hexweaver Totem Shard + 1x Deep Dark Marrow + 1x Ender Pearl
- **Blue Ninja Leggings** — *Tide Legs* (swim speed + knockback resistance)
  - Activate: Swim speed while in water; KB resist always active
  - Effect: +30% swim speed + 20% knockback resistance.
  - Refine 6: x2  |  Refine 12: x3
  - **Recipe:** 1x World Boss Heartstone + 1x Deep Dark Marrow + 1x Hexbrewer's Bile + 1x Diamond + 1x Gold Ingot
- **Blue Ninja Boots** — *Glide Step* (Depth Strider III + landing Speed burst)
  - Activate: Depth Strider always active; Speed burst on landing
  - Effect: Depth Strider III enchantment (real vanilla enchant) + Speed I (3s) on landing.
  - Refine 6: 6s Speed  |  Refine 12: 9s Speed
  - **Recipe:** 1x World Boss Heartstone + 1x Hexbrewer's Bile + 1x Molten Piglin Tusk + 1x Gold Ingot + 1x Lapis Lazuli + 1x Emerald
- **4-Piece Set Bonus:** Every 20s: pulses a 4-block radius (Refine 6: 8, Refine 12: 12) that applies Slowness II for 3s (Refine 6: Slowness III for 6s, Refine 12: Slowness IV for 9s) to nearby enemies.

#### Evoker

**Red Ninja Set**

- **Red Ninja Helmet** — *Blood Focus* (bonus damage at low health)
  - Activate: Passive while below 50% HP
  - Effect: +10% damage while below 50% HP.
  - Refine 6: x2  |  Refine 12: x3
  - **Recipe:** 1x World Boss Heartstone + 1x Bloodforged Gold Shard + 1x Withered Gold Dust + 1x Ender Pearl
- **Red Ninja Chestplate** — *Ember Core* (Regeneration pulse at low health)
  - Activate: Automatic pulse every 15s while below 50% HP
  - Effect: Regeneration I for 8s, every 15s while below 50% HP.
  - Refine 6: Regen II for 16s  |  Refine 12: Regen III for 24s
  - **Recipe:** 1x World Boss Heartstone + 1x Withered Gold Dust + 1x Warpath Sinew + 1x Diamond + 1x Gold Ingot
- **Red Ninja Leggings** — *Scarlet Legs* (Speed I at low health)
  - Activate: Passive while below the HP% threshold
  - Effect: Speed I while below 10% HP.
  - Refine 6: below 15% HP  |  Refine 12: below 20% HP
  - **Recipe:** 1x World Boss Heartstone + 1x Warpath Sinew + 1x Illusory Ichor + 1x Gold Ingot + 1x Lapis Lazuli + 1x Emerald
- **Red Ninja Boots** — *Cinder Step* (Speed I burst on taking damage)
  - Activate: Triggers on taking damage (skipped if Speed already active)
  - Effect: Speed I for 4s when hit.
  - Refine 6: 7s  |  Refine 12: 10s
  - **Recipe:** 1x World Boss Heartstone + 1x Illusory Ichor + 1x Deep Dark Marrow + 1x Lapis Lazuli
- **4-Piece Set Bonus:** Below 50% HP, melee hits (not vs. Deviants) heal you for 10% of that hit's damage dealt (Refine 6: 20%, Refine 12: 30%).

**Grimdark Diamond Set**

- **Grimdark Diamond Helmet** — *Mind Ward* (cleanses Levitation & Wither over time)
  - Activate: Passive — rolled every second of Levitation / every Wither damage tick
  - Effect: 40% chance per second/tick to cleanse Levitation and Wither.
  - Refine 6: 50% chance  |  Refine 12: 60% chance
  - **Recipe:** 1x World Boss Heartstone + 1x Deep Dark Marrow + 1x Cauldron-Bound Essence + 1x Emerald + 1x Iron Ingot
- **Grimdark Diamond Chestplate** — *Spectral Core* (chance to negate projectiles)
  - Activate: Triggers on taking projectile damage (chance-based)
  - Effect: 10% chance to negate incoming projectile dmg.
  - Refine 6: x2  |  Refine 12: x3
  - **Recipe:** 1x World Boss Heartstone + 1x Cauldron-Bound Essence + 1x Molten Piglin Tusk + 1x Iron Ingot + 1x Copper Ingot + 1x Ender Pearl
- **Grimdark Diamond Leggings** — *Arcane Legs* (chance to resist Slowness & Weakness)
  - Activate: Passive — rolled each time Slowness/Weakness would apply
  - Effect: 40% chance to resist Slowness and Weakness.
  - Refine 6: 50% chance  |  Refine 12: 60% chance
  - **Recipe:** 1x World Boss Heartstone + 1x Molten Piglin Tusk + 1x Goldrot Marrow + 1x Copper Ingot
- **Grimdark Diamond Boots** — *Phase Step* (chance to blink away, fully damage-immune)
  - Activate: Triggers on taking damage (chance-based)
  - Effect: 15% chance per hit to blink 3 blocks away + 1s full damage immunity.
  - Refine 6: 20% chance  |  Refine 12: 25% chance
  - **Recipe:** 1x World Boss Heartstone + 1x Goldrot Marrow + 1x Bonecrusher Hide + 1x Ender Pearl + 1x Diamond
- **4-Piece Set Bonus:** Roughly every 30s (Refine 6: ~40s, Refine 12: ~50s): grants Absorption 2 pts/1 heart (Refine 6: 4 pts/2 hearts, Refine 12: 6 pts/3 hearts) and Regeneration I for 10s (Refine 6: Regeneration II for 20s, Refine 12: Regeneration III for 30s).


### Tools

The 8 pure Telekinesis tools (Pickaxe/Axe/Hoe/Shovel skins) have no active skill — they're a
straight vanilla tool with Telekinesis (mined blocks go straight to your inventory). A handful of
other boss-crafted tools (axe/scythe/polearm-shaped) carry a real combat skill instead, listed
alongside them below since the crafting menu groups them together.

#### Piglin Brute

- **Kalam0n's Pickaxe** — no active skill (purely reskinned)
  - Purely reskinned — no active skill.
  - Normal vanilla mining + Telekinesis (blocks go straight to your inventory).
  - Telekinesis: 1
  - **Recipe:** 1x Diamond Pickaxe + 1x Cindermaw Scale + 1x Netherite Ingot
- **Kalam0n's Axe** — no active skill (purely reskinned)
  - Purely reskinned — no active skill.
  - Normal vanilla chopping/combat + Telekinesis (blocks go straight to your inventory).
  - Telekinesis: 1
  - **Recipe:** 1x Diamond Axe + 1x Bloodforged Gold Shard + 1x Netherite Ingot
- **Kalam0n's Hoe** — no active skill (purely reskinned)
  - Purely reskinned — no active skill.
  - Normal vanilla tilling + Telekinesis (blocks go straight to your inventory).
  - Telekinesis: 1
  - **Recipe:** 1x Diamond Hoe + 1x Molten Piglin Tusk + 1x Netherite Ingot
- **Kalam0n's Shovel** — no active skill (purely reskinned)
  - Purely reskinned — no active skill.
  - Normal vanilla digging + Telekinesis (blocks go straight to your inventory).
  - Telekinesis: 1
  - **Recipe:** 1x Diamond Shovel + 1x Cindermaw Scale + 1x Netherite Ingot

#### Zombified Piglin

- **Spade** — no active skill (purely reskinned)
  - Purely reskinned — no active skill.
  - Normal vanilla digging + Telekinesis (blocks go straight to your inventory).
  - Telekinesis: 1
  - **Recipe:** 1x Diamond Shovel + 1x Withered Gold Dust + 1x Netherite Ingot

#### Warden

- **Hoe** — no active skill (purely reskinned)
  - Purely reskinned — no active skill.
  - Normal vanilla tilling + Telekinesis (blocks go straight to your inventory).
  - Telekinesis: 1
  - **Recipe:** 1x Diamond Hoe + 1x Resonant Echo Shard + 1x Netherite Ingot
- **Lumberjack** — *Sunder*
  Cracks a target's defenses wide open. First-ever active axe skill.
  - Activate: Any hit adds a stacking mark on the target (max 3, no cooldown on application)
  - Effect: +8% damage taken from all sources per stack, 5s independent expiry each.
  - Refine 6: +10%/stack  |  Refine 12: +12%/stack
  - **Recipe:** 1x World Boss Heartstone + 1x Hexweaver Totem Shard + 1x Resonant Echo Shard + 1x Gold Ingot + 1x Lapis Lazuli + 1x Emerald
- **Arcanethyst** — *Arcane Echo*
  A crystal-etched blade that bends a strike back through time.
  - Activate: On a fully-charged hit, chance to store an Echo that replays part of this hit's damage ~0.75s later (no cooldown).
  - Effect: 20% chance to echo 50% of the original hit's damage as bonus true damage. On proc: grants you Speed II and the target Slowness X, both for 3s.
  - Refine 6: 25% chance  |  Refine 12: 30% chance
  - **Recipe:** 1x World Boss Heartstone + 1x Hexbrewer's Bile + 1x Bloodforged Gold Shard + 1x Emerald + 1x Iron Ingot
- **Ice Whisper** — *Frostbitten Edge*
  A whisper of frost that saps the strength from any strike.
  - Activate: Every hit weakens the target's own damage output (always-on, no cooldown, no chance).
  - Effect: Weakness V for 3s, refreshed on each hit.
  - **Recipe:** 1x World Boss Heartstone + 1x Warpath Sinew + 1x Illusory Ichor + 1x Ender Pearl + 1x Diamond

#### Witch

- **Lucky Pick** — no active skill (purely reskinned)
  - Purely reskinned — no active skill.
  - Normal vanilla mining + Telekinesis (blocks go straight to your inventory).
  - Telekinesis: 1
  - **Recipe:** 1x Diamond Pickaxe + 1x Hexbrewer's Bile + 1x Netherite Ingot
- **Demigod's Unholy Halberd** — *Impaling Reach*
  Long enough to run two enemies through in the same thrust.
  - Activate: Passive — every hit also strikes a second enemy directly behind the target in a line, if one is standing there (no cooldown).
  - Effect: The second enemy takes 60% of the primary hit's damage.
  - Refine 6: 70% of primary  |  Refine 12: 80% of primary
  - **Recipe:** 1x World Boss Heartstone + 1x Deep Dark Marrow + 1x Hexbrewer's Bile + 1x Ender Pearl + 1x Diamond + 1x Gold Ingot

#### Evoker

- **Soft Pick** — no active skill (purely reskinned)
  - Purely reskinned — no active skill.
  - Normal vanilla mining + Telekinesis (blocks go straight to your inventory).
  - Telekinesis: 1
  - **Recipe:** 1x Diamond Pickaxe + 1x Hexweaver Totem Shard + 1x Netherite Ingot

### Elytra Skins

Every elytra skin has two alternate crafting paths that produce the same item — the Elytra path
(spend a spare vanilla Elytra) or the Membrane path (compress Phantom Membrane into Cases/Bundles
instead of spending an Elytra). Both need the same boss materials on top. See section 6 for what
the Speedster/Tank/Glass Cannon archetypes mean in practice.

#### Tier 1

**Piglin Brute**

- **Mondstadt Wings** — Tier 1 Speedster (Movement Speed +10%, Fall Damage -25%, Safe Fall Distance +4 blocks, Gravity -35%)
  - **Recipe (Elytra path):** 1x Elytra + 1x Molten Piglin Tusk + 1x Withered Gold Dust
  - **Recipe (Membrane path):** 1x Phantom Membrane Case + 2x Phantom Membrane Bundle + 1x Molten Piglin Tusk + 1x Withered Gold Dust
- **Liyue Wings** — Tier 1 Tank (Armor +8, Armor Toughness +2)
  - **Recipe (Elytra path):** 1x Elytra + 1x Cindermaw Scale + 1x Warpath Sinew
  - **Recipe (Membrane path):** 1x Phantom Membrane Case + 2x Phantom Membrane Bundle + 1x Cindermaw Scale + 1x Warpath Sinew
- **Sumeru Wings** — Tier 1 Glass Cannon (Attack Damage +10, Armor -5)
  - **Recipe (Elytra path):** 1x Elytra + 1x Bloodforged Gold Shard + 1x Illusory Ichor
  - **Recipe (Membrane path):** 1x Phantom Membrane Case + 2x Phantom Membrane Bundle + 1x Bloodforged Gold Shard + 1x Illusory Ichor
- **Inazuma Wings** — Tier 1 Speedster (Movement Speed +10%, Fall Damage -25%, Safe Fall Distance +4 blocks, Gravity -35%)
  - **Recipe (Elytra path):** 1x Elytra + 1x Molten Piglin Tusk + 1x Resonant Echo Shard
  - **Recipe (Membrane path):** 1x Phantom Membrane Case + 2x Phantom Membrane Bundle + 1x Molten Piglin Tusk + 1x Resonant Echo Shard

**Zombified Piglin**

- **Fontaine Wings I** — Tier 1 Tank (Armor +8, Armor Toughness +2)
  - **Recipe (Elytra path):** 1x Elytra + 1x Goldrot Marrow + 1x Bloodforged Gold Shard
  - **Recipe (Membrane path):** 1x Phantom Membrane Case + 2x Phantom Membrane Bundle + 1x Goldrot Marrow + 1x Bloodforged Gold Shard
- **Fontaine Wings II** — Tier 1 Speedster (Movement Speed +10%, Fall Damage -25%, Safe Fall Distance +4 blocks, Gravity -35%)
  - **Recipe (Elytra path):** 1x Elytra + 1x Withered Gold Dust + 1x Bonecrusher Hide
  - **Recipe (Membrane path):** 1x Phantom Membrane Case + 2x Phantom Membrane Bundle + 1x Withered Gold Dust + 1x Bonecrusher Hide
- **Frost Wings** — Tier 1 Glass Cannon (Attack Damage +10, Armor -5)
  - **Recipe (Elytra path):** 1x Elytra + 1x Goldrot Marrow + 1x Illusory Ichor
  - **Recipe (Membrane path):** 1x Phantom Membrane Case + 2x Phantom Membrane Bundle + 1x Goldrot Marrow + 1x Illusory Ichor
- **Musa Believix Wings** — Tier 1 Tank (Armor +8, Armor Toughness +2)
  - **Recipe (Elytra path):** 1x Elytra + 1x Withered Gold Dust + 1x Deep Dark Marrow
  - **Recipe (Membrane path):** 1x Phantom Membrane Case + 2x Phantom Membrane Bundle + 1x Withered Gold Dust + 1x Deep Dark Marrow

**Warden**

- **Sheikah Wings** — Tier 1 Glass Cannon (Attack Damage +10, Armor -5)
  - **Recipe (Elytra path):** 1x Elytra + 1x Deep Dark Marrow + 1x Illusory Ichor
  - **Recipe (Membrane path):** 1x Phantom Membrane Case + 2x Phantom Membrane Bundle + 1x Deep Dark Marrow + 1x Illusory Ichor
- **Zonai Wings** — Tier 1 Speedster (Movement Speed +10%, Fall Damage -25%, Safe Fall Distance +4 blocks, Gravity -35%)
  - **Recipe (Elytra path):** 1x Elytra + 1x Direwail Sculk Node + 1x Hexbrewer's Bile
  - **Recipe (Membrane path):** 1x Phantom Membrane Case + 2x Phantom Membrane Bundle + 1x Direwail Sculk Node + 1x Hexbrewer's Bile
- **Zora Wings** — Tier 1 Tank (Armor +8, Armor Toughness +2)
  - **Recipe (Elytra path):** 1x Elytra + 1x Resonant Echo Shard + 1x Bloodforged Gold Shard
  - **Recipe (Membrane path):** 1x Phantom Membrane Case + 2x Phantom Membrane Bundle + 1x Resonant Echo Shard + 1x Bloodforged Gold Shard

**Witch**

- **Descension Wings** — Tier 1 Tank (Armor +8, Armor Toughness +2)
  - **Recipe (Elytra path):** 1x Elytra + 1x Cauldron-Bound Essence + 1x Bonecrusher Hide
  - **Recipe (Membrane path):** 1x Phantom Membrane Case + 2x Phantom Membrane Bundle + 1x Cauldron-Bound Essence + 1x Bonecrusher Hide
- **Feasting Wings** — Tier 1 Glass Cannon (Attack Damage +10, Armor -5)
  - **Recipe (Elytra path):** 1x Elytra + 1x Hexbrewer's Bile + 1x Illusory Ichor
  - **Recipe (Membrane path):** 1x Phantom Membrane Case + 2x Phantom Membrane Bundle + 1x Hexbrewer's Bile + 1x Illusory Ichor
- **First Flight Wings** — Tier 1 Speedster (Movement Speed +10%, Fall Damage -25%, Safe Fall Distance +4 blocks, Gravity -35%)
  - **Recipe (Elytra path):** 1x Elytra + 1x Cauldron-Bound Essence + 1x Deep Dark Marrow
  - **Recipe (Membrane path):** 1x Phantom Membrane Case + 2x Phantom Membrane Bundle + 1x Cauldron-Bound Essence + 1x Deep Dark Marrow

**Ravager**

- **Gerudo Wings** — Tier 1 Glass Cannon (Attack Damage +10, Armor -5)
  - **Recipe (Elytra path):** 1x Elytra + 1x Warpath Sinew + 1x Molten Piglin Tusk
  - **Recipe (Membrane path):** 1x Phantom Membrane Case + 2x Phantom Membrane Bundle + 1x Warpath Sinew + 1x Molten Piglin Tusk
- **Goron Wings** — Tier 1 Tank (Armor +8, Armor Toughness +2)
  - **Recipe (Elytra path):** 1x Elytra + 1x Bonecrusher Hide + 1x Goldrot Marrow
  - **Recipe (Membrane path):** 1x Phantom Membrane Case + 2x Phantom Membrane Bundle + 1x Bonecrusher Hide + 1x Goldrot Marrow
- **BotW Paraglider** — Tier 1 Speedster (Movement Speed +10%, Fall Damage -25%, Safe Fall Distance +4 blocks, Gravity -35%)
  - **Recipe (Elytra path):** 1x Elytra + 1x Splintered Horn + 1x Illusory Ichor
  - **Recipe (Membrane path):** 1x Phantom Membrane Case + 2x Phantom Membrane Bundle + 1x Splintered Horn + 1x Illusory Ichor

**Evoker**

- **Companionship Wings** — Tier 1 Speedster (Movement Speed +10%, Fall Damage -25%, Safe Fall Distance +4 blocks, Gravity -35%)
  - **Recipe (Elytra path):** 1x Elytra + 1x Illusory Ichor + 1x Hexbrewer's Bile
  - **Recipe (Membrane path):** 1x Phantom Membrane Case + 2x Phantom Membrane Bundle + 1x Illusory Ichor + 1x Hexbrewer's Bile
- **Music Wings** — Tier 1 Glass Cannon (Attack Damage +10, Armor -5)
  - **Recipe (Elytra path):** 1x Elytra + 1x Hexweaver Totem Shard + 1x Cindermaw Scale
  - **Recipe (Membrane path):** 1x Phantom Membrane Case + 2x Phantom Membrane Bundle + 1x Hexweaver Totem Shard + 1x Cindermaw Scale
- **Prime Wings** — Tier 1 Tank (Armor +8, Armor Toughness +2)
  - **Recipe (Elytra path):** 1x Elytra + 1x Illusory Ichor + 1x Goldrot Marrow
  - **Recipe (Membrane path):** 1x Phantom Membrane Case + 2x Phantom Membrane Bundle + 1x Illusory Ichor + 1x Goldrot Marrow


#### Tier 2

**Piglin Brute**

- **Yellow Wings** — Tier 2 Speedster (Movement Speed +18%, Fall Damage -45%, Safe Fall Distance +7 blocks, Gravity -55%)
  - **Recipe (Elytra path):** 1x Elytra + 1x Bloodforged Gold Shard + 1x Withered Gold Dust + 1x Splintered Horn + 1x Illusory Ichor
  - **Recipe (Membrane path):** 5x Phantom Membrane Case + 1x Bloodforged Gold Shard + 1x Withered Gold Dust + 1x Splintered Horn + 1x Illusory Ichor
- **Yellow Maple Wings** — Tier 2 Tank (Armor +8, Armor Toughness +3, Knockback Resistance +10%)
  - **Recipe (Elytra path):** 1x Elytra + 1x Molten Piglin Tusk + 1x Splintered Horn + 1x Illusory Ichor + 1x Direwail Sculk Node
  - **Recipe (Membrane path):** 5x Phantom Membrane Case + 1x Molten Piglin Tusk + 1x Splintered Horn + 1x Illusory Ichor + 1x Direwail Sculk Node
- **Royal Wings** — Tier 2 Glass Cannon (Attack Damage +20, Armor -10)
  - **Recipe (Elytra path):** 1x Elytra + 1x Cindermaw Scale + 1x Illusory Ichor + 1x Direwail Sculk Node + 1x Cauldron-Bound Essence
  - **Recipe (Membrane path):** 5x Phantom Membrane Case + 1x Cindermaw Scale + 1x Illusory Ichor + 1x Direwail Sculk Node + 1x Cauldron-Bound Essence

**Zombified Piglin**

- **Blue Wings** — Tier 2 Glass Cannon (Attack Damage +20, Armor -10)
  - **Recipe (Elytra path):** 1x Elytra + 1x Withered Gold Dust + 1x Hexbrewer's Bile + 1x Cindermaw Scale + 1x Warpath Sinew
  - **Recipe (Membrane path):** 5x Phantom Membrane Case + 1x Withered Gold Dust + 1x Hexbrewer's Bile + 1x Cindermaw Scale + 1x Warpath Sinew
- **Leaf Wings** — Tier 2 Speedster (Movement Speed +18%, Fall Damage -45%, Safe Fall Distance +7 blocks, Gravity -55%)
  - **Recipe (Elytra path):** 1x Elytra + 1x Goldrot Marrow + 1x Cindermaw Scale + 1x Warpath Sinew + 1x Illusory Ichor
  - **Recipe (Membrane path):** 5x Phantom Membrane Case + 1x Goldrot Marrow + 1x Cindermaw Scale + 1x Warpath Sinew + 1x Illusory Ichor
- **Maple Wings** — Tier 2 Tank (Armor +8, Armor Toughness +3, Knockback Resistance +10%)
  - **Recipe (Elytra path):** 1x Elytra + 1x Withered Gold Dust + 1x Warpath Sinew + 1x Illusory Ichor + 1x Resonant Echo Shard
  - **Recipe (Membrane path):** 5x Phantom Membrane Case + 1x Withered Gold Dust + 1x Warpath Sinew + 1x Illusory Ichor + 1x Resonant Echo Shard

**Warden**

- **Dark Wings** — Tier 2 Glass Cannon (Attack Damage +20, Armor -10)
  - **Recipe (Elytra path):** 1x Elytra + 1x Deep Dark Marrow + 1x Withered Gold Dust + 1x Splintered Horn + 1x Illusory Ichor
  - **Recipe (Membrane path):** 5x Phantom Membrane Case + 1x Deep Dark Marrow + 1x Withered Gold Dust + 1x Splintered Horn + 1x Illusory Ichor
- **Ripped Cape** — Tier 2 Speedster (Movement Speed +18%, Fall Damage -45%, Safe Fall Distance +7 blocks, Gravity -55%)
  - **Recipe (Elytra path):** 1x Elytra + 1x Direwail Sculk Node + 1x Splintered Horn + 1x Illusory Ichor + 1x Hexbrewer's Bile
  - **Recipe (Membrane path):** 5x Phantom Membrane Case + 1x Direwail Sculk Node + 1x Splintered Horn + 1x Illusory Ichor + 1x Hexbrewer's Bile
- **Insect Wings** — Tier 2 Tank (Armor +8, Armor Toughness +3, Knockback Resistance +10%)
  - **Recipe (Elytra path):** 1x Elytra + 1x Resonant Echo Shard + 1x Illusory Ichor + 1x Hexbrewer's Bile + 1x Cindermaw Scale
  - **Recipe (Membrane path):** 5x Phantom Membrane Case + 1x Resonant Echo Shard + 1x Illusory Ichor + 1x Hexbrewer's Bile + 1x Cindermaw Scale
- **Jet Wings** — Tier 2 Glass Cannon (Attack Damage +20, Armor -10)
  - **Recipe (Elytra path):** 1x Elytra + 1x Deep Dark Marrow + 1x Hexbrewer's Bile + 1x Cindermaw Scale + 1x Goldrot Marrow
  - **Recipe (Membrane path):** 5x Phantom Membrane Case + 1x Deep Dark Marrow + 1x Hexbrewer's Bile + 1x Cindermaw Scale + 1x Goldrot Marrow

**Witch**

- **Blue Demon Wings** — Tier 2 Tank (Armor +8, Armor Toughness +3, Knockback Resistance +10%)
  - **Recipe (Elytra path):** 1x Elytra + 1x Hexbrewer's Bile + 1x Withered Gold Dust + 1x Warpath Sinew + 1x Illusory Ichor
  - **Recipe (Membrane path):** 5x Phantom Membrane Case + 1x Hexbrewer's Bile + 1x Withered Gold Dust + 1x Warpath Sinew + 1x Illusory Ichor
- **Lesser Demon Wings** — Tier 2 Glass Cannon (Attack Damage +20, Armor -10)
  - **Recipe (Elytra path):** 1x Elytra + 1x Cauldron-Bound Essence + 1x Warpath Sinew + 1x Illusory Ichor + 1x Resonant Echo Shard
  - **Recipe (Membrane path):** 5x Phantom Membrane Case + 1x Cauldron-Bound Essence + 1x Warpath Sinew + 1x Illusory Ichor + 1x Resonant Echo Shard
- **Red Demon Wings** — Tier 2 Speedster (Movement Speed +18%, Fall Damage -45%, Safe Fall Distance +7 blocks, Gravity -55%)
  - **Recipe (Elytra path):** 1x Elytra + 1x Hexbrewer's Bile + 1x Illusory Ichor + 1x Resonant Echo Shard + 1x Bloodforged Gold Shard
  - **Recipe (Membrane path):** 5x Phantom Membrane Case + 1x Hexbrewer's Bile + 1x Illusory Ichor + 1x Resonant Echo Shard + 1x Bloodforged Gold Shard
- **True Demon Wings** — Tier 2 Tank (Armor +8, Armor Toughness +3, Knockback Resistance +10%)
  - **Recipe (Elytra path):** 1x Elytra + 1x Cauldron-Bound Essence + 1x Resonant Echo Shard + 1x Bloodforged Gold Shard + 1x Goldrot Marrow
  - **Recipe (Membrane path):** 5x Phantom Membrane Case + 1x Cauldron-Bound Essence + 1x Resonant Echo Shard + 1x Bloodforged Gold Shard + 1x Goldrot Marrow

**Ravager**

- **Green Dragon Wings** — Tier 2 Speedster (Movement Speed +18%, Fall Damage -45%, Safe Fall Distance +7 blocks, Gravity -55%)
  - **Recipe (Elytra path):** 1x Elytra + 1x Warpath Sinew + 1x Direwail Sculk Node + 1x Hexbrewer's Bile + 1x Bloodforged Gold Shard
  - **Recipe (Membrane path):** 5x Phantom Membrane Case + 1x Warpath Sinew + 1x Direwail Sculk Node + 1x Hexbrewer's Bile + 1x Bloodforged Gold Shard
- **Red Dragon Wings** — Tier 2 Glass Cannon (Attack Damage +20, Armor -10)
  - **Recipe (Elytra path):** 1x Elytra + 1x Bonecrusher Hide + 1x Hexbrewer's Bile + 1x Bloodforged Gold Shard + 1x Goldrot Marrow
  - **Recipe (Membrane path):** 5x Phantom Membrane Case + 1x Bonecrusher Hide + 1x Hexbrewer's Bile + 1x Bloodforged Gold Shard + 1x Goldrot Marrow
- **White Dragon Wings** — Tier 2 Tank (Armor +8, Armor Toughness +3, Knockback Resistance +10%)
  - **Recipe (Elytra path):** 1x Elytra + 1x Splintered Horn + 1x Bloodforged Gold Shard + 1x Goldrot Marrow + 1x Illusory Ichor
  - **Recipe (Membrane path):** 5x Phantom Membrane Case + 1x Splintered Horn + 1x Bloodforged Gold Shard + 1x Goldrot Marrow + 1x Illusory Ichor

**Evoker**

- **Angel Wings** — Tier 2 Speedster (Movement Speed +18%, Fall Damage -45%, Safe Fall Distance +7 blocks, Gravity -55%)
  - **Recipe (Elytra path):** 1x Elytra + 1x Illusory Ichor + 1x Bonecrusher Hide + 1x Resonant Echo Shard + 1x Hexbrewer's Bile
  - **Recipe (Membrane path):** 5x Phantom Membrane Case + 1x Illusory Ichor + 1x Bonecrusher Hide + 1x Resonant Echo Shard + 1x Hexbrewer's Bile
- **Guardian Wings** — Tier 2 Tank (Armor +8, Armor Toughness +3, Knockback Resistance +10%)
  - **Recipe (Elytra path):** 1x Elytra + 1x Hexweaver Totem Shard + 1x Resonant Echo Shard + 1x Hexbrewer's Bile + 1x Molten Piglin Tusk
  - **Recipe (Membrane path):** 5x Phantom Membrane Case + 1x Hexweaver Totem Shard + 1x Resonant Echo Shard + 1x Hexbrewer's Bile + 1x Molten Piglin Tusk
- **White Wings** — Tier 2 Glass Cannon (Attack Damage +20, Armor -10)
  - **Recipe (Elytra path):** 1x Elytra + 1x Illusory Ichor + 1x Hexbrewer's Bile + 1x Molten Piglin Tusk + 1x Goldrot Marrow
  - **Recipe (Membrane path):** 5x Phantom Membrane Case + 1x Illusory Ichor + 1x Hexbrewer's Bile + 1x Molten Piglin Tusk + 1x Goldrot Marrow


---

## 10. Materials Glossary

Every gear recipe draws on a universal crafting core plus boss-specific Materials dropped by
World Bosses (a chance per eligible attacker on kill). A recipe always needs 2 Materials from 2
*different* bosses, plus 1-3 currency ore (Diamond, Gold Ingot, Lapis Lazuli, Emerald, Iron Ingot,
Copper Ingot, or Ender Pearl) and the core.

| Material | Boss | Description |
|---|---|---|
| World Boss Heartstone | All World Bosses | A crystallized fragment of a World Boss's essence. Required by every gear-crafting recipe (not tool recipes). Dropped by any World Boss. |
| Molten Piglin Tusk | Piglin Brute | A tusk still radiating forge-heat. Dropped by Cindermaw (Piglin Brute). |
| Cindermaw Scale | Piglin Brute | A scale that never cools. Dropped by Cindermaw (Piglin Brute). |
| Bloodforged Gold Shard | Piglin Brute | Gold quenched in battle, not water. Dropped by Cindermaw (Piglin Brute). |
| Goldrot Marrow | Zombified Piglin | Marrow gone strange with rot and riches. Dropped by Goldrot (Zombified Piglin). |
| Withered Gold Dust | Zombified Piglin | Fine dust, dull with decay. Dropped by Goldrot (Zombified Piglin). |
| Bonecrusher Hide | Ravager | Thick hide, scarred from countless charges. Dropped by Bonecrusher (Ravager). |
| Splintered Horn | Ravager | Cracked clean through by its own force. Dropped by Bonecrusher (Ravager). |
| Warpath Sinew | Ravager | Tough as the ground it charged across. Dropped by Bonecrusher (Ravager). |
| Hexweaver Totem Shard | Evoker | A shard still humming with illager magic. Dropped by Hexweaver (Evoker). |
| Illusory Ichor | Evoker | Shifts color when you're not looking. Dropped by Hexweaver (Evoker). |
| Direwail Sculk Node | Warden | Pulses faintly with a heartbeat that isn't yours. Dropped by Direwail (Warden). |
| Resonant Echo Shard | Warden | Carries a sound from somewhere far below. Dropped by Direwail (Warden). |
| Deep Dark Marrow | Warden | Cold, dense, and faintly luminous. Dropped by Direwail (Warden). |
| Hexbrewer's Bile | Witch | Bottled straight from the cauldron. Dropped by Hexbrewer (Witch). |
| Cauldron-Bound Essence | Witch | Won't stay corked for long. Dropped by Hexbrewer (Witch). |

The World Bosses' individual names above (Cindermaw, Goldrot, Bonecrusher, Hexweaver, Direwail,
Hexbrewer) are simply the flavor names for the Piglin Brute, Zombified Piglin, Ravager, Evoker,
Warden, and Witch boss types respectively — see the Boss Guide (section 8) for their full kits.
