---
name: App Icon Design
description: How the SkinCraft Studio adaptive icon is structured and what it depicts.
---

## Files

- `app/src/main/res/drawable/ic_launcher_background.xml` — Vector: dark navy base (#0A1628) with blue top-right triangle (#1565C0) and teal bottom-left triangle (#00695C).
- `app/src/main/res/drawable/ic_launcher_foreground.xml` — Vector: pixel-art Minecraft character head (skin tone face, dark hair cap, blue eyes with highlights, blocky grin with white teeth) + a diagonal paint-brush badge (gold circle, multi-colour bristles).
- `app/src/main/res/mipmap-anydpi-v26/ic_launcher.xml` — Adaptive icon referencing the two drawables above.

## What NOT to do

- Do NOT point `ic_launcher_foreground.xml` at the JPEG `ic_skin_logo_1784027639947.jpg` — that was the old approach and produces a blurry scaled photo, not a crisp vector icon.
- The foreground vector uses 108×108 viewport. Content should stay within the central 72dp safe zone to avoid clipping on circle/squircle masks.

**Why:** User requested a new app icon; vector adaptive icons scale perfectly across all densities and mask shapes.
