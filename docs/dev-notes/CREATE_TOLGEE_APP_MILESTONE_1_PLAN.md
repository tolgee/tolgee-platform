# Plan: `create-tolgee-app` generator + dev environment (milestone 1)

> **Draft.** Captured for later execution; preparation work needed first
> (see "Open prep work" at the bottom).

## Context

Tolgee Apps PoC is functional but onboarding a new plugin author is hostile:
they have to hand-write `manifest.json`, glue postMessage by reading source,
guess at the JWT shape, copy/paste a tunnel URL into Tolgee on every dev
restart, and reverse-engineer the webhook signature scheme by reading Kotlin.

This milestone delivers a `create-tolgee-app` CLI that scaffolds a working
plugin in one prompt-driven flow, plus a `npm run dev` in the generated
project that "just works" against `app.tolgee.io` — public tunnel, auto-
re-pointing the manifest URL on every restart, hot reload on manifest edits.

**Deferred to later milestones:** deploy targets (milestone 2), test
scaffolding (milestone 3), multi-framework templates, SDK extraction,
asymmetric JWT / JWKS, `tolgee-app doctor`, `add-module` subcommand.

The full research lives at `CREATE_TOLGEE_APP_RESEARCH.md` (sibling file).

## Constraints discovered during exploration

- **No SDK package exists.** The template inlines a small `tolgeeApp.ts`
  with the postMessage `ready`/`init` handshake (copied + cleaned from
  `dev-plugin/src/App.tsx`).
- **No "swap manifest URL" endpoint.** Refresh re-fetches from the
  *existing* URL; with Cloudflare Quick Tunnels giving a new URL on
  every restart, this is the friction point. Milestone adds the
  endpoint.
- **JWT is HMAC-signed** with the platform's shared signing key
  (`AppTokenService.kt`). Claims: `aud="tg.app"`, `sub=userId`,
  `tg.app.inst`, `tg.app.proj`. No JWKS today. Template includes a
  decoder that *reads* the claims for context but does not verify
  signature itself (verification would need the platform secret, which
  plugins don't have); plugins instead authenticate calls back to
  Tolgee with the JWT as a bearer token and let Tolgee verify it.
- **Webhook signature** is a JSON-blob header
  `Tolgee-Signature: {"timestamp": <ms>, "signature": "<hex>"}` where
  the signed payload is `${timestamp}.${rawBody}` HMAC-SHA256. Per-
  install `webhookSecret` lives on the install record. Template includes
  a verifier matching this exact scheme.

## Deliverables

### A. Backend: one new endpoint

`PATCH /v2/organizations/{organizationId}/apps/{installId}/manifest-url`

- Body: `{ manifestUrl: String }`
- Auth: `RequiresOrganizationRole(OWNER)` (matches the rest of
  `OrganizationAppsController`)
- Behavior: fetch the new manifest via `AppManifestFetcher`, assert
  `appId` is unchanged (same rule as `refresh()`), update the existing
  install in place (manifestUrl + all snapshot fields), return the
  updated `AppInstallModel`.
- Reuses `AppInstallService` — pattern mirrors the existing
  `refresh(installId)` method; this is `updateManifestUrl(installId, newUrl)`.

**Files to add/modify** (representative):
- `backend/api/src/main/kotlin/io/tolgee/api/v2/controllers/organization/OrganizationAppsController.kt` — new `@PatchMapping("/{installId}/manifest-url")` controller method.
- `backend/data/src/main/kotlin/io/tolgee/service/apps/AppInstallService.kt` — new `updateManifestUrl()` service method, lifted from `refresh()`.
- `backend/app/src/test/kotlin/io/tolgee/api/v2/controllers/organization/OrganizationAppsControllerTest.kt` — test for the happy path + the `appId mismatch` error case.
- Regenerate `webapp/src/service/apiSchema.generated.ts`.

### B. CLI package: `create-tolgee-app/`

Location: `tolgee-platform/create-tolgee-app/` (same worktree, alongside
`dev-plugin/`). Eventual move to `tolgee-js` on publish.

Stack:
- TypeScript source, bundled with **tsup** to a single `dist/index.js` shebang script
- **@clack/prompts** for the interactive wizard (modern, what Astro/Nuxt use)
- **giget**-free: template lives as `template/` inside the package and is copied with simple file-name + variable substitution (mustache-style `{{appId}}`)
- Executable name in `package.json` `bin`: `create-tolgee-app` → resolves to `npm create tolgee-app@latest`

Wizard questions (in order):
1. App name (kebab-case, becomes `id` in manifest, becomes target dir name)
2. Modules to scaffold (multi-select from the 10 known module types — `project-dashboard-page`, `translation-tools-panel`, `key-edit-tab`, `key-action`, `translation-action`, `modal`, `bulk-action`, `translations-toolbar-action`, `project-menu-action`, `shortcut`)
3. Webhook events (multi-select from a curated list of common `ActivityType` values; "none" is OK)
4. Tolgee URL (default `https://app.tolgee.io`, free-text override for self-hosted)
5. Install deps now? (y/n, detects pm from `npm_config_user_agent`)
6. Git init + initial commit? (y/n)

Critical files in `create-tolgee-app/`:
- `src/index.ts` — entry, runs wizard, copies template
- `src/copy.ts` — recursive copy with variable substitution
- `src/registry.ts` — module/event registry (single source of truth shared by template's manifest builder + the wizard)
- `template/` — see next section
- `package.json` with `bin`, `tsup` build, `engines.node >=20`
- `README.md`

### C. Generated project template

Mirrors `dev-plugin/` minus PoC-specific routes (no `emoji`, no `state` API). Stack: Vite + React + TypeScript + Express, same ports (5180 frontend, 5181 backend).

Skeleton:
```
<app-name>/
  manifest.json                  ← populated from wizard answers
  package.json                   ← scripts: dev, build, register, refresh
  tsconfig.json
  .env.example                   ← TOLGEE_URL, ORG_ID, INSTALL_ID, CLIENT_SECRET, WEBHOOK_SECRET, TOLGEE_PAT
  .gitignore                     ← .env.local, .tolgee-app.json, node_modules
  README.md                      ← 5-line quickstart + what each env var means
  vite.config.ts
  src/
    main.tsx                     ← React root for module entries
    tolgeeApp.ts                 ← postMessage glue (ready/init handshake, token + selection accessors)
    modules/<key>/index.tsx      ← one folder per selected module with a stub <H1>{title}</H1>
  server/
    index.ts                     ← Express app, mounts routes from below
    signature.ts                 ← Tolgee-Signature header verifier (HMAC over "${timestamp}.${rawBody}")
    routes/
      webhook.ts                 ← POST /webhook, signature-verified, switch on activityData.type
      decorators.ts              ← POST /decorators with CORS preflight for TOLGEE_URL
  scripts/
    dev.ts                       ← orchestrator (see part D)
    register.ts                  ← interactive: prompts for PAT, posts to /v2/organizations/{org}/apps
    refresh.ts                   ← POSTs to /apps/{installId}/refresh (manual trigger)
```

### D. Dev orchestrator (`scripts/dev.ts` in generated project)

Single command (`npm run dev`) that:

1. Loads `.env.local`, validates required vars with `zod` (friendly error if missing).
2. Spawns Vite (port 5180) and Express (port 5181) via the existing `concurrently` pattern.
3. Starts a **Cloudflare Quick Tunnel** via the `cloudflared` npm package
   (`JacobLinCool/node-cloudflared`); captures the assigned
   `https://<random>.trycloudflare.com` URL, points it at port 5180.
4. If `.tolgee-app.json` (gitignored, persisted by `npm run register`)
   contains an `installId`:
   - PATCHes `/v2/organizations/{orgId}/apps/{installId}/manifest-url`
     with `{ manifestUrl: "<tunnel>/manifest.json" }` using the stored
     PAT. Endpoint is the one added in part A.
5. If no `installId` yet: print a colorized one-liner instructing
   `npm run register`, exit gracefully (don't block — they can still
   develop locally without Tolgee while iterating UI).
6. Logs all incoming requests (path, method, latency) to stdout for
   visibility.
7. Watches `manifest.json` — on change, POSTs `/apps/{installId}/refresh`
   so Tolgee re-fetches it without a tunnel-URL swap.
8. Tears down the tunnel on `SIGINT`.

`scripts/register.ts` is a separate one-time bootstrap that:
- Prompts for a Tolgee PAT (or reads from `TOLGEE_PAT` env)
- Asks which organization to install into (lists via `GET /v2/organizations`)
- POSTs `manifestUrl` to `/v2/organizations/{org}/apps`
- Writes `{ installId, organizationId, clientSecret, webhookSecret }` to `.tolgee-app.json`

### E. Tunnel integration

- Dependency: `cloudflared` npm package (auto-installs binary, no homebrew step)
- No account / no signup required for Quick Tunnels
- Document a `TOLGEE_DEV_TUNNEL_URL=https://stable.example.com` env override for users who set up their own Named Tunnel — the orchestrator skips Cloudflare boot when set

## Out of scope (deferred)

- `npm run deploy` to Cloudflare/Fly — milestone 2
- Playwright smoke test + JWT contract tests — milestone 3
- Multi-framework templates (Hono, Next.js, Vanilla TS)
- SDK extraction (`@tolgee/apps-sdk` published package)
- Asymmetric JWT / JWKS endpoint
- `tolgee-app doctor`, `add-module`, mock-Tolgee
- Auto-discovery of `organizationId` (PAT might map to multiple orgs — milestone 1 just asks)
- Webhook replay protection (timestamp window check) — note in README

## Verification

End-to-end smoke (in order):

1. Build the CLI:
   `cd tolgee-platform/create-tolgee-app && npm install && npm run build`
2. Run it locally:
   `node dist/index.js my-test-plugin` — verify wizard, verify project tree.
3. In the generated project, `npm install` then `npm run register`:
   verify it asks for a PAT, lists orgs, posts a register call,
   writes `.tolgee-app.json`.
4. `npm run dev` — verify all three processes start, verify
   `cloudflared` prints a `*.trycloudflare.com` URL, verify the PATCH
   to `manifest-url` succeeds, verify Tolgee's `GET /v2/projects/{id}/apps`
   now lists the install with the new tunnel URL.
5. In Tolgee UI: enable the test plugin on a project, navigate to a
   module the wizard scaffolded — verify iframe loads, verify
   `tolgee-app:init` handshake completes (console log inside the
   stub module).
6. Edit `manifest.json` (e.g. change a module title), save —
   verify the dev orchestrator POSTs refresh, verify the Tolgee UI
   picks it up on reload.
7. Restart `npm run dev` (Ctrl-C then re-run) — verify the new
   tunnel URL gets PATCHed in *without* requiring re-register. This
   is the milestone's headline DX win.
8. Send a webhook by editing a translation in Tolgee — verify the
   signature verifier accepts it, verify the typed handler stub fires.

Backend tests:
- `./gradlew :server-app:test --tests "io.tolgee.api.v2.controllers.organization.OrganizationAppsControllerTest" --console=plain`
- Should cover: PATCH happy path, appId-mismatch rejected,
  unauthorized (non-owner) rejected.

---

## Open prep work (to figure out before executing this plan)

_Fill in as we decide._
