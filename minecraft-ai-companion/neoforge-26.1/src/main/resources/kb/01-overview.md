# Overview

Everything in this kb/ folder is knowledge the AI companion can draw on when answering player
questions — in addition to server-info.md. Put whatever detail server-info.md doesn't have room
for here: game systems, lore, item/recipe lists, boss guides, event rules, whatever your server
actually has.

**How this gets used:** every file in kb/ is split into one chunk per heading (any level, "#"
through "######"). When a player asks something, only the chunks that actually look relevant to
their question (by heading match or word overlap) get pulled into the AI's prompt — not the whole
folder every time — so feel free to write as much as you need across as many files as you like.
This file's first top-level heading (the one you're reading now) is always included, as general
framing, so it's a good place for a short "what is this server" summary.

**Organizing multiple files:** name them so they sort in a sensible order (e.g. "01-overview.md",
"02-commands.md", "03-events.md") — the number is just for your own readability, it doesn't need
to mean anything to the bot.

**Writing style that scores well:** give things clear, exact names in headings and **bold** text
(e.g. a boss name, an item name, a command) — those are matched directly against what a player
types, so "## Ender Dragon" matches a lot better than folding the name into a paragraph.

---

## Example: a made-up custom item

*(Delete this section — it's just here to show the format. Replace it, and everything above
except the format/organizing notes, with your own server's actual information.)*

- **Example Sword** — a placeholder item to show the format.
  - Right-click to do a placeholder effect.
  - Crafted from 1 stick + 2 placeholder ingots.

A player asking "what does the Example Sword do?" would match this section because "Example
Sword" appears as bold text here.
