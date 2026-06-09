# `create-tolgee-app` — Research & Recommendations

> Scoping notes for a future plugin-generator CLI. Not yet built. Captured here so we can iterate against it instead of re-doing the research.

## 1. Tunneling for dev

For Tolgee Apps the tunnel is non-negotiable because webhooks come from Tolgee's backend, not the user's browser. The iframe modules technically work over `http://localhost` if the user is testing against a Tolgee that the same browser can reach, but as soon as someone wants to test against `app.tolgee.io`, the same public URL is the only sensible default. So the tunnel needs to be: zero-account, free forever, scriptable from `npm run dev`, and accept inbound HTTPS to a random hostname.

| Tool | Free tier | Persistent URL on free | Account needed | Node integration | Status 2026 |
|---|---|---|---|---|---|
| **Cloudflare Quick Tunnel** (`cloudflared --url`) | Truly free, no caps for HTTP/HTTPS | No (random `*.trycloudflare.com`) | No | `cloudflared` (JacobLinCool) and `untun` npm wrappers | Actively maintained, used by Shopify CLI as the default |
| Cloudflare Named Tunnel | Free | Yes (requires domain on CF) | Yes | Same wrappers | Same as above; more setup |
| ngrok | Restrictive in 2026 (DDEV publicly considered dropping it Feb 2026); single static domain only on paid | No (random unless paid) | Yes (authtoken) | First-class `@ngrok/ngrok` SDK, no binary needed | Pivoting to "Developer Gateway"; free tier increasingly squeezed |
| localtunnel | Free | Custom subdomain attempt, often taken | No | npm package exists | Frequent outages, "open-source bitrot" |
| bore | Free, self-hostable | If you host the server | No (for public server) | Rust binary, no Node SDK | Healthy but TCP-only, no HTTPS terminating |
| tunnelmole | Free | Random subdomain | No | npm | Niche, smaller community |
| Pinggy | Free, requires nothing installed (SSH) | Paid only | No | No real SDK | Nice DX, but SSH-only is fragile inside a `dev` script |

**Recommendation: bundle the [`cloudflared` npm package by JacobLinCool](https://github.com/JacobLinCool/node-cloudflared) as the default tunnel.** It auto-installs the binary on first run (no Homebrew/Cargo step), exposes a typed `bin`/`tunnel` API, and Cloudflare's free Quick Tunnels have no caps and no account. This is exactly the choice Shopify CLI made as their default, and it's the one tunnel where webhooks (server-to-server) and iframe loads (browser-to-tunnel) both work reliably for free.

Allow opt-out via env: `TOLGEE_DEV_TUNNEL=ngrok|cloudflared|none` and a `--tunnel-url` override (mirroring `shopify app dev`) so power users can plug in a Named Tunnel with a static domain for stable webhook URLs.

Avoid `untun` as the primary; it's still 0.1.x and just wraps Quick Tunnels with less control than `cloudflared`. Keep `@ngrok/ngrok` in mind only if Tolgee later wants observability features (request replay) — its SDK is excellent but the free tier is hostile.

## 2. Deployment target

What plugins need: a small Node HTTP server (webhooks, decorators, optional plugin REST API) + static iframe bundle, ability to hold a tiny bit of state (per-install settings, API tokens), and a `npm run deploy` that does not require a credit card to demo.

| Platform | Free | Cold start | Stateful | CLI deploy | Docker | DB add-ons |
|---|---|---|---|---|---|---|
| **Fly.io** | $5 trial only (free tier gone in 2024) | 300ms–2s, scale-to-zero | Yes (Machines + Volumes) | `flyctl deploy`, generates `fly.toml` | First-class | Postgres, LiteFS, Upstash |
| **Render** | Free web service (sleeps after 15min, 30–60s cold start), free PG dies after 30 days | Bad on free | Yes | `render` CLI + `render.yaml` blueprints | Yes | Managed PG, Redis |
| Railway | $5 one-time trial, then $5/mo | Always-on (no scale-to-zero) | Yes | `railway up` | Yes | PG, Redis, Mongo |
| **Cloudflare Workers + Pages** | Generous (100k req/day free), Durable Objects + D1 free tiers | <5ms | Yes via Durable Objects | `wrangler deploy` | No (V8 isolates) | D1 (SQLite), KV, R2, DO |
| Vercel | Free hobby, paid starts $20/seat | Edge ~ms; Node fn ~100ms | No native state | `vercel deploy` | No | Partner integrations |
| Deno Deploy | Free tier | Edge ~ms | Deno KV | `deployctl` | No | KV |
| Koyeb | 1 free nano service + $5.50 credit/mo | ~1s | Yes | `koyeb` CLI | Yes | PG (paid) |
| Heroku | No free tier | n/a | Yes | `git push heroku` | Yes (via buildpacks) | PG, Redis |

**Recommend two defaults, picked by an interactive wizard question:**

1. **Cloudflare Workers + Pages** as the "small/serverless" default. Pricing matches a plugin's traffic shape (mostly idle, spiky on webhooks), Durable Objects + D1 give real state for free, JWT verification via [`jose`](https://github.com/panva/jose) works natively in Workers, and `wrangler` deploy is a single command. The generator drops `wrangler.toml`, a `worker/` entry, a `pages/` directory for iframe assets, and a `.dev.vars.example`.
2. **Fly.io** as the "real Node app" default for plugins that need long-lived processes, native modules (Playwright, sharp, headless Chrome for translation screenshots), or Postgres. The generator drops `fly.toml` + a minimal `Dockerfile` (Node 22 alpine, multistage with static asset copy) and a `flyctl launch --copy-config --no-deploy` hook.

Render is a tempting third because its free tier still exists and is friendlier than Fly to first-timers, but the 30–60s cold start makes webhooks from Tolgee miss their first delivery — bad first-run experience. Mention it in docs but don't generate config for it by default.

Skip Heroku entirely; the user mentioned it but it's actively user-hostile (no free tier, slow dyno boots, expensive add-ons) and nothing it does is better than Fly.

## 3. How comparable plugin systems scaffold

**Crowdin Apps** (the closest analog). No `create-crowdin-app` CLI exists — they hand users a `git clone` of a Next.js starter. The starter does the right things: JWT verification middleware using `jose`, dynamic `manifest.json` route, OAuth credentials in `.env`, deploy target is Vercel. There's no tunnel. **Lesson: their model is good, but the absence of a CLI is the obvious gap Tolgee can fill, and the absence of a tunnel is the obvious DX win.**

**Figma — `create-figma-plugin`** (yuanqing). esbuild-powered, sub-second rebuilds via a `watch` script, prompts for a template (vanilla, Preact, Preact+Tailwind, React, FigJam widget). Configuration lives under a `figma-plugin` key in `package.json`. No tunnel (Figma plugins run inside the editor, not over HTTP). **Lesson: fast iteration via esbuild + a tight template menu beats a giant kitchen-sink starter.**

**Slack CLI** (`slack create`, v4.0 shipped April 2026). Templates split into "Starter app", "Automation", "AI agent". `manifest.json` is canonical, and `slack run` registers the app to a chosen workspace. Auto-upgrades itself on dev machines but never in CI or project-local installs. **Lesson: the template categories matter (boring vs AI vs automation), and the CLI must never auto-upgrade in CI.**

**Atlassian Forge** (`forge tunnel`). The tunnel is built in and uses Cloudflare under the hood — confirms our Cloudflare choice. But Forge has a sharp edge: **manifest changes are not picked up by the tunnel; you must re-run `forge deploy`**. Code changes auto-rebundle; manifest changes don't. **Lesson to avoid: Tolgee's `npm run dev` should watch `manifest.json` (or its source) and re-POST it to Tolgee on change, so the author never has to "re-install" the app while iterating.**

**Shopify CLI** (`app dev`, v4.0 May 2026). Defaults to a Cloudflare Quick Tunnel; `automatically_update_urls_on_dev = true` rewrites the partner dashboard URLs on every dev run. `--tunnel-url` accepts a custom tunnel. **Lesson: lift this exact pattern. The dev command should re-register the manifest URL with Tolgee on every boot.**

**GitHub Apps / Probot** (`create-probot-app`). Templates `basic-js`, `basic-ts`, `checks-js`, `git-data-js`, `deploy-js`. Webhook payload typing is the big win — every event is fully typed. No tunnel; users wire up smee.io manually. **Lesson: ship typed webhook payloads, don't make users define event shapes themselves.**

Patterns to lift: Shopify's auto-URL-rewrite, Forge's bundled tunnel, Probot's typed event payloads, Figma's esbuild watch loop, Slack's category-split template prompt, Crowdin's `jose`-based JWT middleware.

## 4. Recommended `create-tolgee-app` feature list

### Interactive wizard (use [`@clack/prompts`](https://www.npmjs.com/package/@clack/prompts), not Inquirer — modern, 4KB, TS-native, what Astro/Nuxt/Svelte's CLIs use in 2026)

1. **App name + identifier** (kebab-case, used in manifest)
2. **Frontend framework**: `React + Vite` (default), `Next.js` (good for SSR'd iframe pages + dynamic manifest), `Vanilla TS` (smallest)
3. **Backend**: `Hono` (default — runs on Node, Workers, Bun, Deno; clean middleware story), `Express` (for users who want familiarity), `None` (frontend-only plugin)
4. **Modules to scaffold** (multiselect, prefills boilerplate file + manifest entry per choice): `project-dashboard-page`, `translation-tools-panel`, `key-edit-tab`, `key-action`, `translation-action`, `modal`, `bulk-action`, `translations-toolbar-action`, `project-menu-action`, `shortcut`
5. **Webhook events** (multiselect — drives both manifest entries and pre-typed handler stubs): translation.created, translation.updated, key.created, etc.
6. **Deploy target**: `Cloudflare Workers/Pages` (default), `Fly.io`, `None / I'll figure it out`
7. **Tolgee target**: `app.tolgee.io` (default), `Self-hosted` (asks for URL — stored as `TOLGEE_URL` in `.env`)
8. **Package manager**: detect from `npm_config_user_agent`, only ask if ambiguous
9. **Git init + first commit**: yes/no
10. **Install deps now**: yes/no

### Files generated

- `manifest.ts` — manifest authored in TypeScript with a [Zod](https://github.com/colinhacks/zod) schema imported from `@tolgee/apps-sdk`. `npm run build:manifest` emits `dist/manifest.json` and `tsc`-checks the shape. **This is a big DX win** — Crowdin and Slack make you author raw JSON; Zod-typed authoring catches typos before install.
- `src/modules/<module>/` — one folder per selected module with an iframe entry React component prewired with the `@tolgee/apps-sdk` postMessage handshake and JWT context.
- `src/server/` — Hono app with prewired routes: `GET /manifest.json` (dynamic), `POST /webhooks` (signature-verified), `POST /decorators` (CORS preflight handled), per-module `GET /<module>` (iframe HTML).
- `src/server/middleware/verifyTolgeeJwt.ts` — calls `createRemoteJWKSet(new URL(TOLGEE_URL + '/.well-known/jwks.json'))` from [`jose`](https://github.com/panva/jose), validates `iss`, `aud`, `exp`, `nbf`. Cached + auto-refreshing.
- `src/server/middleware/verifyWebhookSignature.ts` — HMAC-SHA256 over raw body, `crypto.timingSafeEqual` comparison. Assumes Tolgee sends a `X-Tolgee-Signature` header; coordinate the exact header name with the Tolgee backend team.
- `src/shared/events.ts` — typed webhook event union (`type WebhookEvent = TranslationCreated | KeyCreated | …`), generated from the same Zod schemas the Tolgee backend uses (publish from `@tolgee/apps-sdk`).
- `.env.example` — `TOLGEE_URL`, `TOLGEE_APP_ID`, `TOLGEE_APP_SECRET`, `WEBHOOK_SIGNING_SECRET`, `PORT`.
- `vite.config.ts` or equivalent — esbuild-fast, with HMR for iframe content.
- Deploy config: `wrangler.toml` **or** `fly.toml` + `Dockerfile` based on wizard answer. Both include a sample `deploy` script in `package.json`.
- `.github/workflows/deploy.yml` — push-to-main deploys to the chosen target. (Skip if user said "None".)
- `tolgee-app.config.ts` — the dev server's config: which manifest source to watch, dev port, tunnel preference.
- `README.md` with a 5-line quickstart, and explicit "what's a webhook signature" / "what's the JWT for" sections.
- `.gitignore`, `tsconfig.json`, `eslint.config.js` (flat config, modern), `.prettierrc`.

### `npm run dev`

A single orchestrator that:

1. Loads `.env`, validates required vars with Zod (fails loudly with a friendly message if `TOLGEE_APP_ID` is missing).
2. Starts the local server (Hono on `localhost:3000` by default).
3. Starts Vite/esbuild watch for iframe assets, with HMR.
4. Starts a Cloudflare Quick Tunnel via the `cloudflared` npm package; captures the assigned `https://<random>.trycloudflare.com` URL.
5. Watches `manifest.ts` → recomputes manifest JSON → if the app is registered, POSTs the new tunnel URL + manifest to Tolgee's app registration endpoint with the developer's PAT. **This is the Shopify-style auto-update; Forge's mistake was not doing it.**
6. On first run, if the app isn't registered yet, opens the Tolgee install URL in the browser (`open` package or platform fallback), preloaded with the manifest URL and a deeplink back to the project.
7. Streams structured logs of incoming requests (path, JWT subject, latency) — small in-terminal request log à la ngrok inspector but free.
8. On Ctrl-C, tears down the tunnel, optionally unregisters dev URLs from Tolgee.

### `npm run deploy`

Per chosen target:

- **Cloudflare**: `wrangler deploy` for the worker + `wrangler pages deploy ./dist/ui` for static assets. Reads secrets from `.env` and uploads with `wrangler secret put` on first deploy. Prints the resulting URL and offers to update the production manifest URL in Tolgee.
- **Fly**: `flyctl deploy`. On first run, runs `flyctl launch --copy-config --no-deploy` to register the app, then `flyctl secrets set` from `.env`, then deploys. Same "update prod manifest URL in Tolgee?" prompt at the end.
- **None**: builds to `dist/` and exits with a printed checklist of what env vars to wire up wherever they're going.

### Security boilerplate the plugin author should NEVER write

- **Tolgee JWT verification** against Tolgee's JWKS (`/.well-known/jwks.json`) using `jose.createRemoteJWKSet` with caching. Validate `iss === TOLGEE_URL`, `aud === appId`, `exp`, `nbf`. Apply to every iframe page route and every decorator endpoint.
- **Webhook signature verification** (HMAC-SHA256, raw body, `timingSafeEqual`). The Hono middleware reads raw body before any JSON parser sees it — this is the single most common webhook bug across providers.
- **CORS preflight** for decorator endpoints, allowing only `https://app.tolgee.io` (or `TOLGEE_URL`) origins by default with `Access-Control-Allow-Credentials: true` for the JWT.
- **CSP / iframe** headers on iframe routes: `Content-Security-Policy: frame-ancestors <TOLGEE_URL>`. This is easy to forget and locks the iframe to only Tolgee.
- **Replay protection** on webhooks: 5-minute window using the JWT `iat` or a webhook `timestamp` header; reject older.

### Bonus things plugin authors will love but won't ask for

- **Typed webhook handlers**: `app.onWebhook('translation.created', (payload) => …)` where `payload` is inferred from a shared Zod schema. Probot-grade DX.
- **Manifest validation as a pre-commit hook** via `lefthook` or `simple-git-hooks` — `tolgee-app validate-manifest` fails the commit if a referenced module entry doesn't exist on disk, an `events` array contains a non-existent event name, or a URL is non-HTTPS.
- **`npx tolgee-app doctor`** — checks env vars, Tolgee reachability, tunnel health, JWKS fetchability, common misconfigurations. Borrowed from `next info` / `tailwindcss doctor`.
- **Module generator post-init**: `npx tolgee-app add-module bulk-action` adds a new module to manifest + scaffolds the file. Saves users from re-running the wizard.
- **Local mock Tolgee** for offline dev: a tiny fixture server that simulates webhook deliveries and JWT issuance, so tests don't need a real Tolgee. Forge's `forge tunnel --debug` does something similar and developers love it.
- **Built-in `e2e/` Playwright smoke test** that spins up the dev server, opens an iframe page in a fake-Tolgee shell, and asserts the postMessage handshake succeeds. New plugin authors break the handshake constantly; catching it in CI saves hours.
- **`tolgee-app logs --prod`** to tail logs from the chosen deploy target without learning each provider's CLI.

## 5. Hard problems to flag for follow-up

These are blockers that need a Tolgee-side decision before `create-tolgee-app` can ship:

1. **Manifest registration UX.** Shopify and Forge both have a "partner dashboard"; Tolgee will need one too, and the protocol for "here's my new dev manifest URL, swap it in" needs to be a real Tolgee API. Without it, step 5 of `npm run dev` is a manual copy-paste — exactly the friction this CLI is trying to remove. Coordinate this with the Tolgee backend team before shipping.
2. **JWT issuance contract.** What exactly is in the JWT (subject, aud, scopes, project id, user id, install id)? Is there a separate token for the iframe context vs the decorator POST? Lock this down and publish the spec inside `@tolgee/apps-sdk` so the verifier middleware can be a real type, not a `Record<string, unknown>`.
3. **Webhook signature scheme.** Is it HMAC-SHA256 over raw body (Stripe/Shopify style) or a signed JWT (GitHub-style)? Either is fine, but pick before writing the verifier or you'll bake the wrong assumption into hundreds of generated projects.
4. **State for installs.** Plugins will need to persist "user X installed me in project Y with these settings" somewhere. The CLI scaffolds the table, but the schema (and whether Tolgee provides install IDs at all) is a Tolgee-side design question.
5. **Multi-tenancy on a single deploy.** A plugin author probably wants one Fly app serving every customer install. The manifest needs to be the same JSON for all of them, but per-install state needs an install ID in every JWT. This is again a Tolgee-side contract, not something the CLI can paper over.

## Sources

- [ngrok Alternatives 2026 — fxTunnel](https://www.fxtun.dev/blog/free-ngrok-alternatives-2026/)
- [Top 10 Ngrok alternatives in 2026 — Pinggy](https://pinggy.io/blog/best_ngrok_alternatives/)
- [awesome-tunneling on GitHub](https://github.com/anderspitman/awesome-tunneling)
- [Cloudflare Quick Tunnels docs](https://developers.cloudflare.com/cloudflare-one/networks/connectors/cloudflare-tunnel/do-more-with-tunnels/trycloudflare/)
- [`cloudflared` npm package (JacobLinCool)](https://github.com/JacobLinCool/node-cloudflared)
- [`untun` (unjs)](https://github.com/unjs/untun)
- [`@ngrok/ngrok` JavaScript SDK](https://www.npmjs.com/package/@ngrok/ngrok)
- [Heroku's Dead — Railway vs Render vs Fly.io 2026](https://techsy.io/en/blog/railway-vs-render-vs-fly-io)
- [Fly.io Pricing 2026 — Kuberns](https://kuberns.com/blogs/flyio-pricing/)
- [Render Free Tier docs](https://render.com/docs/free)
- [Koyeb Pricing FAQ](https://www.koyeb.com/docs/faqs/pricing)
- [Cloudflare Workers vs Vercel vs Deno Deploy 2026 — ProPicked](https://propicked.com/blog/cloudflare-workers-vs-vercel-vs-deno-deploy-2026-edge-comparison)
- [Crowdin Apps Quick Start](https://support.crowdin.com/developer/crowdin-apps-quick-start/)
- [Crowdin App Descriptor](https://support.crowdin.com/developer/crowdin-apps-app-descriptor/)
- [Forge tunneling docs](https://developer.atlassian.com/platform/forge/tunneling/)
- [Forge CLI reference](https://developer.atlassian.com/platform/forge/cli-reference/)
- [Shopify CLI 4.0 release notes (May 2026)](https://no7software.co.uk/blog/shopify-cli-4-engineering-migration-2026)
- [Shopify `app dev` docs](https://shopify.dev/docs/api/shopify-cli/app/app-dev)
- [Slack CLI v4.0.0 release notes (April 2026)](https://docs.slack.dev/changelog/2026/04/10/slack-cli/)
- [Create Figma Plugin docs](https://yuanqing.github.io/create-figma-plugin/quick-start/)
- [Probot development docs](https://probot.github.io/docs/development/)
- [`jose` JWT/JWKS library](https://github.com/panva/jose)
- [Webhook Signature Verification 2026 — HookRay](https://hookray.com/blog/webhook-signature-verification-2026)
- [`@clack/prompts` vs Inquirer vs Ink 2026 — PkgPulse](https://www.pkgpulse.com/guides/ink-vs-clack-vs-enquirer-interactive-cli-nodejs-2026)
- [Zod](https://github.com/colinhacks/zod)
