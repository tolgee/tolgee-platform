# Short-Term Observations

Quick dumps of potentially useful observations. To be processed during `/consolidate-memory`.

---

## [2026-01-12 18:05] Codebase: Gradle project structure

Tolgee uses flat Gradle module names, not nested. Use `:core:compileKotlin` not `:backend:core:compileKotlin`. The backend directory structure doesn't map to Gradle project paths.

