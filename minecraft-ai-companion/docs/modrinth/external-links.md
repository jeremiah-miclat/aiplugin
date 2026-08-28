# Modrinth "External links" field

No repo/Discord URL was supplied when this doc was written, so the fields below are placeholders —
fill them in, then paste into Modrinth's **Links** settings.

| Modrinth field | What it expects | Value |
|---|---|---|
| **Source code** | Link to the public repo (GitHub/GitLab/etc.) | `[FILL IN: e.g. https://github.com/<you>/minecraft-ai-companion]` |
| **Issue tracker** | Where players/server admins report bugs | `[FILL IN: e.g. https://github.com/<you>/minecraft-ai-companion/issues — or leave blank if you don't want public bug reports yet]` |
| **Discord invite** | Optional community/support server | `[FILL IN, or leave blank — not required]` |
| **Wiki** | Optional, if you host guide content outside Modrinth | `[FILL IN, or leave blank — the guides in this docs/modrinth/ folder can just live in the description instead]` |

Notes:

- If the project isn't pushed to a public repo yet, it's fine to submit for review with only the
  description/icon/gallery/disclosures filled in and add Source/Issues later — none of those four
  link fields are required by Modrinth's checklist (they're all listed with the 💡 "Suggestion"
  icon in the checklist, not the ✱ "Required" one).
- If you do make the repo public, don't commit `config.yml` with a real API key baked in anywhere
  in its history — see the security notes in [full-guide.md](full-guide.md).
