# Translation Memory — Frontend UI Proposal

This document is the working proposal for the frontend of the managed Translation Memory feature (PR tolgee/tolgee-platform#3596). It is grounded in the actual Tolgee patterns observed in the running app and the Glossary frontend, plus the wireframes provided.

## Goals

1. Mirror the Glossary UX wherever it makes sense — same list item layout, same dialog shape, same sidebar placement — so users don't learn a new pattern.
2. Respect the existing Tools Panel pattern inside the Translations editor — the TM panel is already there for the free plan; the Business plan version just swaps the data source and optionally adds "source TM" attribution.
3. Separate backend-ready UI (list, create, assign, browse, disconnect dialog, keep-or-delete) from designer-dependent polish (micro-copy, illustrations, icons).

## Navigation placement

### Organization sidebar
Insert **"Translation memories"** directly after **"Glossaries"** in `BaseOrganizationSettingsView` navigation. Both belong to "Linguistic assets" conceptually. No need for a section header yet — two items is fine.

```
Organization profile
Organization members
Member permissions
Glossaries                 ← existing
Translation memories       ← new
Apps
Single Sign-On (SSO)
Subscriptions
Invoices
```

### Project settings
Two options, both defensible:

- **Option A (compact)** — a new "Translation memory" **row** on the existing *Advanced* tab, matching the wireframe style. Just a toggle + "TM Main is used as primary. Manage TMs in org TM settings" helper + link.
- **Option B (dedicated page)** — a new **Advanced → Translation memory** sub-page (separate route) with the connected-TMs list + drag-reorder from wireframe #4.

**Recommendation**: **both**. The Advanced tab gets a summary row that links to the dedicated page. The dedicated page holds the actual config (priority ordering, connect/disconnect, per-assignment read/write). This matches how the existing "QA checks" section handles its own deep config today.

### In-editor (Translations view)
No structural change. The existing **Translation Memory** panel in the Tools Panel stays in place, with one behavior change:
- When the project's org has `Feature.TRANSLATION_MEMORY` enabled, the panel calls `POST /v2/projects/{id}/suggest/managed-translation-memory` instead of the classic `/suggest/translation-memory`.
- Optionally: each suggestion row can show which TM it came from (small badge near the similarity score) — needs the backend to return `translationMemoryName`, which it currently doesn't.

---

## Screen 1: Organization → Translation memories (list)

### Empty state
Exactly mirror the Glossaries empty state observed in the live app:

```
┌─────────────────────────────────────────────────────────────────┐
│                                                                 │
│                      No translation memory yet                  │
│                                                                 │
│    Create a translation memory to reuse existing translations   │
│    across projects. Check Best practice.                        │
│                                                                 │
│                       [📚 illustration]                         │
│                                                                 │
│                   [ + NEW TRANSLATION MEMORY ]                  │
│                                                                 │
└─────────────────────────────────────────────────────────────────┘
```

Copy: "No translation memory yet" · "Create a translation memory to reuse existing translations across projects." · Primary button "New translation memory".

### Populated state
Mirror `GlossaryListItem` layout exactly — same grid, same paddings, same hover, same kebab menu:

```
 TRANSLATION MEMORIES                             [search]  [ + NEW ]

 ┌──────────────────────────────────────────────────────────────────┐
 │ Main               │ All projects       │ 🇬🇧 EN       │ ⋮        │
 │ 12,450 entries     │                    │              │          │
 ├──────────────────────────────────────────────────────────────────┤
 │ Product A          │ 2 projects         │ 🇬🇧 EN       │ ⋮        │
 │ 3,280 entries      │                    │              │          │
 ├──────────────────────────────────────────────────────────────────┤
 │ Product B          │ 1 project          │ 🇬🇧 EN       │ ⋮        │
 │ 1,890 entries      │                    │              │          │
 ├──────────────────────────────────────────────────────────────────┤
 │ Web app Acko       │ Project-owned      │ 🇬🇧 EN       │ ⋮ dis    │
 │ 248 entries        │                    │              │          │
 └──────────────────────────────────────────────────────────────────┘
```

Columns (same grid template as Glossary): **Name + count** | **Assigned projects** | **Source language** | **⋮ menu**.

- Click row → opens the single-TM view (Screen 2).
- Kebab menu: *Edit name* / *Delete* / (for Shared) *Export TMX* / *Import TMX*.
- PROJECT-type TMs are listed inline with a "Project-owned" badge and disabled kebab (you can't rename or delete them via the org page — only via the project they belong to). **Question for designer**: should we hide project-owned TMs entirely in the org list? Wireframe #2 implied they *are* visible (row showing "All" or project names in the second column).

### Create dialog
Mirror the Glossary create dialog:

```
┌──────────────── New translation memory ────────────────┐
│ Name                                                   │
│ [ Marketing TM                                       ] │
│                                                        │
│ Assigned to projects  (optional)                       │
│ [ Select projects                              ▼ ]     │
│                                                        │
│ Source language                                        │
│ [ English 🇬🇧                                  ▼ ]     │
│                                                        │
│ ⓘ If you don't assign the TM to any project now,      │
│   you can connect it later from project settings.      │
│                                                        │
│                              [ CANCEL ]  [ CREATE ]    │
└────────────────────────────────────────────────────────┘
```

## Screen 2: Organization → Single TM content browser

This is the most-designed screen and needs the most input. Wireframe #3 proposed a **pivoted** view (one row per source with multiple target columns) labelled "+ TERM". My backend stores entries as **flat** `(source, target, language)` triples — which is how Phrase/Crowdin display it.

### Recommended layout (flat, matches backend)

```
 Projects / Main                                                          ← breadcrumb
 ┌────────────────────────────────────────────────────────────────────────┐
 │ Main (DEFAULT)                        Used in projects: All            │
 │ Source language: English 🇬🇧                                            │
 └────────────────────────────────────────────────────────────────────────┘

 [ search source or target… ]   [ Target: All languages ▼ ]
                                          [ ⟳ IMPORT TMX ] [ ⇩ EXPORT TMX ] [ + ENTRY ]

 ┌────────────────────────────────────────────────────────────────────────┐
 │ SOURCE                   │ TARGET                 │ LANG │ KEY    │ ⋮ │
 ├────────────────────────────────────────────────────────────────────────┤
 │ Hello world              │ Hallo Welt             │ de   │ hello  │ ⋮ │
 │ Hello world              │ Bonjour le monde       │ fr   │ hello  │ ⋮ │
 │ Add to cart              │ In den Warenkorb       │ de   │ cart   │ ⋮ │
 │ Welcome to our store     │ Willkommen …           │ de   │ —      │ ⋮ │ ← snapshot (no key)
 └────────────────────────────────────────────────────────────────────────┘

                          < 1 2 3 ... 42 >
```

- **Search**: substring match on source OR target (backend already supports this via `?search=`).
- **Language filter**: single-select or multi-select; backend supports single `?targetLanguageTag=` today; multi is a small backend tweak if needed.
- **Columns**: Source, Target, Language, Key name (nullable — shows "—" for snapshot/TMX/manual entries), kebab.
- **Kebab**: *Edit / Delete*.
- **Click row**: opens edit dialog (same fields as "+ Entry").
- **Import / Export TMX**: disabled (grey) in v1 since backend is not yet there — show tooltip "Coming soon". OR hide entirely until Week 4 lands TMX.

### Alternative pivoted layout

If the designer insists on wireframe #3's pivoted view, we'd need to decide:

- How to collapse duplicate `source_text` across entries. Group by source, show one cell per target language, take the most-recent if multiple entries exist for the same (source, lang).
- What happens when the user clicks a cell — edit inline? open side drawer?
- How to render sources that only have a translation in one language (lots of empty cells).

This view is **prettier** but **lossier** — it hides that the backend may hold multiple entries per (source, lang), and it's harder to implement search hits and navigation inside. **Recommendation**: go flat first, revisit pivoted as a view toggle if users ask for it.

### Add / edit entry dialog
Small form: `Source text` (textarea), `Target text` (textarea), `Target language` (dropdown filtered to project languages, or free BCP-47 input for shared TMs not tied to a project). Validation: required, 10000 char max (matches backend `@Size`).

## Screen 3: Project settings → Advanced (compact row)

Render a new section between "Translations" and "Export and file formats":

```
 Translation memory (shared)
 ───────────────────────────────────────────────────────────────
 Project memory      On  [ ●━━ ]
 The project's own memory stores every translation you make.
 Managed from Organization settings.

 Connected shared memories
   1. Main           12,450 entries    Default          [ Configure ]
   2. Product A       3,280 entries                     [ Configure ]
                                                        + Add shared memory
```

- Each row of connected shared memories shows priority number, name, entry count, optional "Default" badge, and a "Configure" button that opens the dedicated Translation memory sub-page (Screen 4) OR an inline popover for quick toggles.
- The "Project memory" toggle is always on; it's disabled with a tooltip explaining "The project memory cannot be disconnected — it captures your own translations".
- "+ Add shared memory" opens a modal listing the org's SHARED TMs not yet assigned, with read/write/priority pickers.

## Screen 4: Project settings → Advanced → Translation memory (dedicated page)

This is the wireframe #4 layout — the full config page with drag-to-reorder:

```
 Projects / Demo project / Settings / Translation memory

 TRANSLATION MEMORY
 Configure which translation memories this project uses.

 CONNECTED MEMORIES                                      [ + ADD MEMORY ]

 ┌───┬──────────────────────────────────────────────────────────┐
 │ ⋮⋮│ 1  Web app Acko  PROJECT  • 248 entries                  │
 │   │    Read ✓  Write ✓                                       │
 └───┴──────────────────────────────────────────────────────────┘
 ┌───┬──────────────────────────────────────────────────────────┐
 │ ⋮⋮│ 2  Main  DEFAULT  • 12,450 entries · Shared with: All    │
 │   │    Read ✓  Write ✓    [ Edit access… ]  [ Remove ]       │
 └───┴──────────────────────────────────────────────────────────┘
 ┌───┬──────────────────────────────────────────────────────────┐
 │ ⋮⋮│ 3  Product A  • 3,280 entries · Shared with: 2 projects  │
 │   │    Read ✓  Write ✗    [ Edit access… ]  [ Remove ]       │
 └───┴──────────────────────────────────────────────────────────┘

 ⓘ Priority order: memories are queried top-to-bottom when looking
   up suggestions. Drag the handle to reorder.
```

- **Drag handle** on the left (matches wireframe #4).
- **Project-owned row** is pinned at any position (still reorderable for priority) but its Remove button is disabled.
- **Read/write toggles** inline (checkbox or eye/pencil icon) — opens a popover for "Edit access" where priority can also be edited numerically.
- **Remove button** on shared TMs triggers the keep-or-delete dialog (Screen 5).

## Screen 5: Disconnect dialog (keep-or-delete)

```
┌────────── Disconnect Main from this project? ──────────┐
│                                                        │
│ Main will no longer provide suggestions for this       │
│ project. You have two options:                         │
│                                                        │
│   ◉ Keep a copy of the entries in this project's      │
│     own memory (≈ 12,450 entries will be copied)       │
│                                                        │
│   ○ Disconnect without copying                         │
│                                                        │
│                                                        │
│                              [ CANCEL ]  [ DISCONNECT ]│
└────────────────────────────────────────────────────────┘
```

- Radio-style choice with a single confirm button; default is "Keep a copy".
- The entry count is a new field (`entryCount` or similar) the backend should return on the TM list endpoint — trivial addition.
- "Disconnect" button uses destructive color only when the "without copying" radio is selected. Otherwise neutral.
- On success, toast: "Main disconnected. X entries copied to the project memory."

## Screen 6: Translations editor — TM panel (in-place)

No visual redesign. The TM panel already exists (screenshot 04 from the exploration). Two tiny changes:

1. **Data source swap**: when the org has `Feature.TRANSLATION_MEMORY`, call the managed endpoint; otherwise keep classic.
2. **Per-row source badge** (optional, backend work required): small pill next to each suggestion showing which TM it came from.

No copy change on free plan; keeps saying "Translation memory". (The free-plan rename "Similar translations" is a separate concern handled in a different PR per earlier conversation.)

---

## Questions for the designer

### Navigation
1. Sidebar — **Translation memories** as a sibling to Glossaries, or a collapsible "Linguistic assets" group containing both?
2. Project settings — compact row on Advanced **plus** dedicated sub-page, or only one of them?

### Org list (Screen 1)
3. Should PROJECT-type TMs appear in the org list? If yes, visually distinct (badge, faded) or identical to shared?
4. Is "Main" in wireframe #2 meant as a **specific default TM** (a new concept) or just an example of a user-named shared TM? The `DEFAULT` badge is non-obvious — the backend currently has no "default TM per org" concept.
5. Kebab menu items: confirm *Edit name / Delete / Import TMX / Export TMX* is the full set. Should "Assign to project…" also be available here?
6. Row click → single TM view, or expose "Open" in the kebab menu only?

### Single TM browser (Screen 2)
7. **Pivoted vs flat**: prefer flat (matches backend + Phrase/Crowdin), or insist on pivoted (wireframe #3)?
8. Columns for the flat layout — should Key name be visible by default or hidden behind a "show columns" menu?
9. Inline cell edit or modal edit?
10. Bulk-select + bulk-delete — needed for v1?
11. Empty-state copy + illustration.
12. IMPORT / EXPORT TMX buttons — show as disabled "Coming soon" in v1, or hide entirely until Week 4?

### Project config (Screens 3 + 4)
13. Is the "Configure" on Advanced tab a link to the sub-page, or a popover?
14. Read / write access — visible per row or hidden behind "Edit access…"?
15. Does the designer want "entry count" and "shared with X projects" text inline, or on hover (tooltip)?
16. Drag-reorder on mobile — do we need a numeric fallback?
17. "+ Add memory" — modal with available shared TMs, or inline autocomplete?

### Disconnect dialog (Screen 5)
18. Radio + single confirm (my proposal) vs two confirm buttons "Keep and disconnect" / "Disconnect without keeping"?
19. Should we show the entry count before confirming?
20. Post-disconnect toast copy?

### Translations editor (Screen 6)
21. Should shared-TM suggestions show a source-TM badge per row? (Requires backend to return TM name in the suggestion payload.)
22. On free plan — is the panel label staying "Translation memory" or renaming to "Similar translations" (plan-gating rename is scoped to a separate PR, but confirm copy).
23. Empty-state copy for the Business plan ("No matches found across your translation memories.").

### Copy / icons
24. Icon for "Translation memory" in sidebar and tools panel. Current Glossary uses BookClosed; TM could use a repeating-text / brain / database icon. Designer pick.
25. Confirm copy strings for all headings / buttons / placeholders so we can seed Tolgee before the UI is reviewed.

### TMX (Week 4)
26. File-size limit for import?
27. Import progress — inline progress bar, toast, or full-page status?
28. Conflict modes — "overwrite existing" / "ignore" / "merge"?
29. Export filter — whole TM, or per-language?

---

## What I can build now without designer input

Only **Screen 6** — the tools-panel data source swap. Specifically:

1. Modify `webapp/src/views/projects/translations/ToolsPanel/panels/TranslationMemory/TranslationMemory.tsx` to conditionally call the managed endpoint when `useEnabledFeatures().isEnabled('TRANSLATION_MEMORY')` returns true.
2. Keep all existing UI — same panel, same row layout, same "Nothing found" fallback.
3. No new components, no copy changes.
4. Small test adjustment if any test asserts the URL.

Everything else (Screens 1–5) needs at least a few of the questions above answered before meaningful code lands.
