# Glossary Keeper — Tolgee App example

Watches translation changes and proposes glossary entries, shown on a **project dashboard
page** where you can accept them (per row or all at once) into a glossary.

How it works:

1. A `SET_TRANSLATIONS` webhook fires when a translation changes.
2. **Haiku** (`detectSuspicious`) decides whether the change hinges on a domain term that
   should be in the glossary.
3. If so, **Sonnet** (`suggestGlossaryEntry`) drafts a glossary entry (term + translation +
   description), stored as a pending suggestion.
4. The dashboard lists pending suggestions. **Accept** writes the term + translation into the
   chosen glossary via the org-level glossary API — using the app's own install token
   (`glossary.edit` scope), no personal access token required.

If the project has no glossary, the dashboard shows an info block; with one or more, it shows a
target-glossary dropdown.

## Run

```bash
npm install
cp .env.example .env.local   # set ANTHROPIC_API_KEY
npm run register             # install the app into your Tolgee org (grants the glossary scopes)
npm run dev                  # vite + server + tunnel
```

Requires a Tolgee instance with the **Glossary** (Enterprise) feature enabled.

## Test

```bash
npm test   # vitest — pure server logic (model-output parsing, suggestion store)
```
