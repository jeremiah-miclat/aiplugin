# Modrinth publishing kit — AI Companion

Everything needed to satisfy Modrinth's publishing checklist for this project, currently at
version **1.0.0** (MIT-licensed per the Fabric/Forge/NeoForge manifests). Each item below maps
straight to a box on Modrinth's "Publishing checklist" panel.

| Checklist item | What to do |
|---|---|
| ✱ **Upload a version** | Use the already-built jars in `../../releases/1.0.0/<platform>/[<mc-version>/]`. Create one Modrinth version entry per platform build (Modrinth lets one version support multiple loaders/game versions if the jar is identical across them, like Paper's; Fabric/Forge/NeoForge need a separate version entry per Minecraft version since they're separate builds — see the root `README.md`'s Releases section for why). |
| ✱ **Add a description** | Paste [`description.md`](description.md) into the Description field (it's the single combined blob — intro, full setup guide, and Groq API key guide all in one, since Modrinth only takes one paste). Paste [`summary.md`](summary.md) into the short Summary field. |
| 💡 **Add an icon** | Export [`branding/icon.svg`](branding/icon.svg) to PNG — see [`branding/EXPORT-INSTRUCTIONS.md`](branding/EXPORT-INSTRUCTIONS.md) — and upload it. |
| 💡 **Feature a gallery image** | Export [`branding/gallery-overview.svg`](branding/gallery-overview.svg) (and optionally [`branding/gallery-platforms.svg`](branding/gallery-platforms.svg) as a second image) the same way, then upload and feature one. |
| 💡 **Check content disclosures** | No separate disclosures field to paste into — its content is already folded into `description.md`'s "Content disclosures" section, so this ships as part of the one description paste. |
| 💡 **Add external links** | Fill in the placeholders in [`external-links.md`](external-links.md) (source/issues/Discord — none were supplied when this kit was written) and paste into Modrinth's Links settings. All optional per Modrinth (marked 💡, not ✱). |
| ⚖️ **Submit for review** | Manual button click on Modrinth once everything above is filled in — not something that can be done from here. |

## Also in this folder

- [`groq-api-key-guide.md`](groq-api-key-guide.md) — standalone copy of the step-by-step free Groq
  API key guide, kept for reference/reuse elsewhere (e.g. a repo wiki page). Its content is already
  folded into `description.md`'s "Get a free Groq API key" section — if you edit one, mirror the
  change in the other.
- [`full-guide.md`](full-guide.md) — standalone copy of the complete install/config reference for
  all four platforms. Its content is already folded into `description.md`'s "Full setup guide"
  section for the same reason — keep edits in sync across both.
- [`content-disclosure.md`](content-disclosure.md) — standalone copy of the content disclosure
  text. Its content is already folded into `description.md`'s "Content disclosures" section —
  keep edits in sync across both.

## One more thing worth doing before or shortly after publishing

**There's no `LICENSE` file in this repo yet**, even though `fabric.mod.json`/`mods.toml`/
`neoforge.mods.toml` already declare `"license": "MIT"` (Paper's `plugin.yml`/`pom.xml` declare no
license at all — worth aligning too). Modrinth requires picking a license for the project, and MIT
is already the de-facto answer here — add a standard `LICENSE` file at the repo root with your own
name/org as the copyright holder (not guessed here, since that's a real legal detail) so the
declared license and an actual license file agree.
