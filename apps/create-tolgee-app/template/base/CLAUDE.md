# {{name}} — Tolgee App

This is a **Tolgee App**: a hosted web plugin that extends the Tolgee localization platform with
custom UI (iframe modules), reacts to project events (webhooks), and decorates translation rows.
It was scaffolded by `create-tolgee-app`.

## How this project is wired

- `manifest.template.json` — declares what the app contributes (modules, scopes, webhooks,
  `decoratorsUrl`). `__BASE_URL__` is substituted with the live URL at request time. Edit this to
  add/remove UI surfaces.
- `src/` — the iframe modules (React). They use `@tolgee/apps-sdk/browser`.
- `server/` — an Express backend exposing `/manifest.json`, `/webhook`, and `/decorators`. It uses
  `@tolgee/apps-sdk/server`.
- `npm run dev` runs Vite + the server + a dev tunnel; `npm run register` installs the app on a
  Tolgee instance (one-time). Run `dev` first, then `register` in a second terminal.

## Typesafety — verify after every change

This project is strict TypeScript (`strict`, `noUnusedLocals`, `noUnusedParameters`). The SDK is
fully typed, so **lean on the types and check them after every change**:

```bash
npm run typecheck   # tsc -b — fast, no build output; run this after any edit
```

Do not consider a change done until `npm run typecheck` passes. Prefer the typed SDK entry points
over hand-rolled `fetch`/types:

- **REST calls** — use `createTolgeeAppClient(ctx)` (`@tolgee/apps-sdk/browser`). It's a typed client;
  endpoints, params, and responses are checked against Tolgee's OpenAPI schema.
- **Webhooks** — use `receiveWebhook(...)` to verify + parse, then `onWebhook(payload, 'EVENT', cb)`
  to narrow the payload to that event's type. Don't cast webhook bodies by hand.
- **Manifest** — the `AppManifest` types from `@tolgee/apps-sdk` describe every field; keep
  `manifest.template.json` consistent with them.

## Where to look when building

- **Docs**: https://docs.tolgee.io/apps — introduction, setup, full API reference, and tutorials.
- **SDK types**: `node_modules/@tolgee/apps-sdk/dist/*.d.ts` — the authoritative signatures available
  in this project right now.
- **Deep context** (optional, recommended for AI-assisted work): run

  ```bash
  npm run pull-context
  ```

  This clones the relevant Tolgee sources into `.context/` (gitignored). After it runs, look in:
  - `.context/tolgee-platform/apps/tolgee-apps-sdk/src/` — SDK source (exact behavior of every helper).
  - `.context/tolgee-platform/apps/example-apps/dev-plugin/` — a full activity-monitoring example
    (webhooks, decorators, panel, dashboard).
  - `.context/tolgee-platform/apps/example-apps/back-translate/` — a full LLM quality-check example.
  - `.context/documentation/tolgee-apps/` — the documentation source.

  When unsure how a feature behaves, read the SDK source and the example apps in `.context/` — they
  are the ground truth.

## Conventions

- Keep the server's webhook route registered **before** any JSON body parser — webhook signatures are
  verified over the raw request bytes.
- Decorator and custom endpoints must return the SDK's CORS headers (`tolgeeAppCorsHeaders()`).
- After backend edits, re-check the manifest and run `npm run typecheck`.
