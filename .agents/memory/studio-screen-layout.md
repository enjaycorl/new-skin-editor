---
name: Studio Screen Layout
description: How the SkinCraft Studio screen is structured and key tab/mode decisions.
---

## Layout hierarchy (top → bottom, inside a Column)

1. **Header bar** — EXPORT + IMPORT (left) | ← back + "Skin Editor" + project name (center) | SAVE (right)
2. **Main viewport Box (weight=1f)** — holds either:
   - `selectedTab == 1` → 3D full-screen canvas (default)
   - `selectedTab == 0` → 2D pixel editor full-screen canvas
   - Floating left toolbar (draw tools)
   - Floating right toolbar (color, grid, brush, 3D/2D toggle, zoom)
   - Body panel Card overlay anchored `Alignment.BottomCenter` (shown only when `showBodyPanel == true`)
3. **Bottom centre row** (dark semi-transparent) — Person icon (triggers `showBodyPanel`) + Eye icon (toggles `showOuterLayerOnly`)
4. **Red action bar** — "Edit ✏" (in 3D mode, click → go to 2D) / "← 3D Preview" (in 2D mode, click → go back to 3D)

## Key decisions

- `selectedTab = 1` (3D) is the **default** when opening the Studio. Rationale: matches reference app UX where 3D is the primary view.
- The body panel trigger was moved from the bottom-right corner of the viewport Box to the persistent bottom-centre row, so it stays visible in both 3D and 2D modes.
- The body panel Card itself is still rendered *inside* the viewport Box (`align(Alignment.BottomCenter)`), which allows it to float correctly over the viewport.
- `showOuterLayerOnly` (outer layer / jacket visibility) is now toggled from the Eye icon in the bottom-centre row.

**Why:** Reference image shows a prominent 3D skin view with "Edit" at the bottom, matching standard skin-editor app conventions.
