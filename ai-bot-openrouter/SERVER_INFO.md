# RIFTFORGED Server — Reference

This is the ONLY source of truth for the in-game bot. If a player's question
isn't answered by something in this file, the bot must say it doesn't have
that information — never guess, never search the web.

## Basics
- Server name: RIFTFORGED (Test Server)
- Theme: Claims | Economy | Auction | Discord
- Gamemode: survival, Difficulty: easy
- PvP: disabled server-wide
- Whitelist: off, Max players: 100
- Spawn protection radius: 16 blocks
- Online-mode is OFF (cracked-friendly) — see Registration below

## Registration & Login (AuthMe)
- First time joining: `/register <password> <password>` (type it twice)
- Every session after that: `/login <password>`
- Password: 5–30 characters, no spaces/control characters
- Common weak passwords (123456, password, qwerty, etc.) are rejected
- You can't move or chat until you're registered/logged in — you'll be
  kicked after 30 seconds if you don't
- Login sessions last 24 hours, so you won't need to log in again right away
- `/email` recovery command exists but is not currently functional

## Spawn & Homes (EssentialsX)
- `/spawn` — go to the server spawn
- New players get a starter kit at spawn (`newbiekit`): diamond armor and
  sword, a golden shovel, golden apples and emeralds
- `/sethome <name>`, `/home <name>` — you get 5 homes
- No public warps are set up
- Starting balance is $0 — you earn money via playtime and trading (below)
- Other kits exist (tools, dtools, notch, color, firework) but are staff-set,
  not self-service unless a staff member gives access

## Teleport Requests (TPAPlugin)
- `/tpa <player>` — request to teleport to someone
- `/tpahere <player>` — request someone teleport to you
- `/tpaccept` / `/tpadeny` — respond to a request (or click the chat buttons)
- 5 second delay before teleporting, request expires after 60 seconds
- Moving or taking damage cancels a pending teleport

## Land Claims (GriefPrevention)
- Works in the Overworld only (not Nether/End)
- You start with 100 claim blocks (a claim needs at least 100, i.e. 5x5)
- Claim blocks do NOT accrue automatically over time — you must buy more
- `/buyclaimblocks` — costs 2.0 currency per block; selling blocks back
  gives 1.0 currency per block
- Max accrued claim blocks: 80,000
- Claims auto-expire after 60 days of inactivity (unless you have 10,000+
  total or 5,000+ bonus claim blocks, which exempts you)
- Standard claim commands apply: `/claim`, `/trust`, `/untrust`,
  `/abandonclaim`, `/claimslist`
- Note: PvP is off server-wide anyway, so claim PvP protection rarely matters

## Auction House
- `/ah` — open the auction house
- `/ah sell <price>` — list an item at a fixed price (Buy-It-Now), lasts 48h
- `/ah bid <amount>` — list an item for bidding, lasts 2h; each new bid must
  be at least 25% higher than the last
- 1% tax is taken on sales
- Max 10 active listings per player at once

## Death & Graves (GravesPRO)
- When you die, a grave is created holding your items
- You get a Recovery Compass pointing to your own grave
- Right-click the grave to loot it — you can loot your own grave instantly;
  other players must wait 5 minutes before they can loot it
- Graves last 1 hour before they expire and drop their items on the ground
- Max 5 graves per player at once

## Making Money
- TimeIsMoney pays you for playtime: $50/hour played (up to $1000/day), or
  $100/hour (up to $10,000/day) for VIP-ranked players
- Being AFK does not earn money
- You can also earn money by selling items on the Auction House

## The Rift (Riftforged — custom server event)
- Build a closed rectangle out of emerald blocks near an eligible generated
  structure (a village, mansion, etc.)
- Right-click a frame block with flint & steel to open a rift
- Only one rift can be active on the whole server at a time
- The event runs in stages: Phase 1 → Phase 2 → a World Boss fight → a loot
  chest; mob counts scale with the size of the structure
- Killing the boss always drops an Enchantment Stone, plus a chance at a
  Gear Core, Materials, or a Socket Stone (used for gear crafting)
- Totems of Undying auto-use from your inventory when you'd die, with a
  14 second cooldown before another can trigger

## Resource Pack
- A resource pack is force-applied when you join (declining it or failing
  to download it will get you kicked)
- It includes RIFTFORGED's custom "Seizon" textures (elytra/gear) plus
  custom music

## Discord
- Join the Discord: https://discord.gg/5h695G2Edx
- In-game global chat is linked two-way with the Discord server's chat
  channel — messages show up on both sides automatically
- Linking your Discord account is optional, not required to play
