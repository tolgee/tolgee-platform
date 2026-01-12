# Short-Term Observations

Quick dumps of potentially useful observations. To be processed during `/consolidate-memory`.

---

## [2026-01-12 18:05] Codebase: Gradle project structure

Tolgee uses flat Gradle module names, not nested. Use `:core:compileKotlin` not `:backend:core:compileKotlin`. The backend directory structure doesn't map to Gradle project paths.

## [2026-01-12 18:45] Architecture: Interface naming convention in core module

In the `core` module's new architecture, interface names use "I" as a **pronoun** (meaning "me"), not as an abbreviation for "Interface". The pattern is "I <verb> <noun>":
- `IQueryProjects` = "I query projects"
- `IFetchTranslations` = "I fetch translations"
- `IFetchProjects` = "I fetch projects"

This reads as a first-person statement of what the component does. Avoid names like `IProjectQueries` where "I" could be mistaken for "Interface".

