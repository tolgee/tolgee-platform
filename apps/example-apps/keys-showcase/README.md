# Keys Showcase — Tolgee App example

A minimal **project dashboard page** app. It renders in a sandboxed iframe inside Tolgee's
project dashboard, receives a scoped token over `postMessage`, and calls Tolgee's REST API to
list **10 localization keys** of the current project — key name, namespace, and the
base-language translation.

That is the whole alpha loop in one small app:

1. The Express server publishes `manifest.json`, declaring one `project-dashboard-page` module
   and the scopes the app needs (`keys.view`, `translations.view`).
2. Tolgee fetches the manifest, and once the app is enabled for a project, renders the page in
   an iframe and posts it a `TolgeeAppContext` — a scoped token, the API URL, the project id,
   and the current theme.
3. The page calls Tolgee with that token (`createTolgeeAppClient`) and follows Tolgee's
   light/dark mode (`applyTolgeeTheme` + `onThemeChanged`), so it looks native.
4. `npm run token` shows the same app reading the same keys from a plain backend script — no
   browser involved.

## Prerequisites

A Tolgee dev server with apps turned on. In your `application.yaml` (or as env vars):

```yaml
tolgee:
  apps:
    enabled: true
    # Needed only because this app runs on localhost. Production instances keep
    # this off, which is why a remote Tolgee needs the dev tunnel below.
    allow-local-addresses: true
```

The app assumes Tolgee is at `http://localhost:8718`; override with `TOLGEE_URL`.

## Install and run

```bash
npm install --prefix ../..   # from apps/ — this is an npm workspace of it
cp .env.example .env.local   # optional; every value has a default
npm run dev                  # vite on :5180 + manifest server on :5181 + dev tunnel
```

- `http://localhost:5180` — the iframe page (Vite)
- `http://localhost:5181/manifest.json` — the manifest Tolgee fetches

With Tolgee on the same machine no tunnel is needed: it fetches `localhost` directly, and the
tunnel process says so and stays out of the way.

## Reaching the app from a remote Tolgee

Tolgee fetches the manifest and loads the iframe **from its own server**, so pointing
`TOLGEE_URL` at staging, a preview environment or production means localhost URLs are useless to
it — and those instances run with `tolgee.apps.allow-local-addresses` disabled, so they reject
them outright.

For a non-localhost `TOLGEE_URL`, `npm run dev` opens a
[Cloudflare quick tunnel](https://developers.cloudflare.com/cloudflare-tunnel/) in front of Vite
(downloading the `cloudflared` binary on first use) and publishes the public URLs to
`.tolgee-dev/tunnel.json`. Vite proxies `/manifest.json` to Express, so one public hostname
covers both the manifest and the iframe.

The hostname changes on every restart. The server waits for it before registering, and
registration repoints the existing install, so restarting `npm run dev` is all it takes for
Tolgee to follow along.

`TOLGEE_DEV_TUNNEL=none` in `.env.local` forces the tunnel off and falls back to
`http://localhost:5180`.

`cloudflared` is a **dev dependency** — it is a local-development convenience, so
nothing that deploys this app pulls its binary down. Without it installed the
tunnel process says so and leaves Vite and the app server running.

## Connecting the app to Tolgee

### Manual mode (default)

`npm run dev` just serves the manifest. Register it yourself:

1. Open Tolgee → **Organization → Apps → Add app**.
2. Paste the manifest URL:

   ```
   http://localhost:5181/manifest.json
   ```

3. Confirm the requested scopes.

### Auto-connect mode

Set the registration secret and the server self-registers on boot:

```bash
cat >> .env.local <<'EOF'
TOLGEE_APP_REGISTRATION_SECRET=<tolgee.apps.registration-secret from the server>
EOF

npm run dev
```

This registers a **native** app — one owned by no organization. Making it
available to an organization is a separate, admin-only decision: in Tolgee go to
**Administration → Apps**, open the app's **Organizations** dialog and grant the
organizations that may use it. Only then can a project owner enable it for a
project.

(Setting `TOLGEE_ORGANIZATION_SLUG` as well installs the app into that single
organization instead, skipping the admin step. Usually you don't want that.)

On the **first** registration Tolgee issues the app's credentials. It shows the client secret
once and stores only its hash, so the SDK writes the install record straight to
`.tolgee-dev/install.json` (gitignored) instead of printing it:

```
Auto-connect: registered install 12 on http://localhost:8718 as a native (server-wide) app.
  Its credentials are stored in .../keys-showcase/.tolgee-dev/install.json (gitignored) — nothing to copy; `npm run token` reads them from there.
```

Nothing to copy. On later boots the app is already registered, so the server only repoints the
existing install at the current manifest URL — which is exactly what makes a fresh tunnel
hostname take effect; the stored secret is left untouched.

Set `TOLGEE_APP_CLIENT_ID` and `TOLGEE_APP_CLIENT_SECRET` only when you deploy the app somewhere
that injects secrets properly — the environment wins over the local file, and setting either one
makes the SDK ignore the file entirely.

If self-registration fails, the server logs why and keeps serving the manifest, so you can
always fall back to the manual flow.

## Enabling the app for a project

Registering installs the app into the organization; it still has to be turned on per project:

**Project → Settings → Apps → Keys Showcase → enable**

The **Keys Showcase** item then appears in the project's dashboard menu.

## Machine-to-machine demo (`npm run token`)

The "cron job" story: an app backend that reads Tolgee on its own, without a user or a browser.

```bash
# in .env.local — the only thing you have to set
TOLGEE_PROJECT_ID=1

npm run token
```

The credentials come from `.tolgee-dev/install.json`, written when the app registered, so
there is nothing else to wire up. The script exchanges them for an access token
(`fetchAppAccessToken`) and prints the project's first 10 keys with their base-language
translations. If the app has never registered — and no `TOLGEE_APP_CLIENT_ID` /
`TOLGEE_APP_CLIENT_SECRET` are set — it says so and points at both ways to fix it.

## Changing what the app contributes

`server/manifest.template.json` is the source of truth. Keep `baseUrl` as `__BASE_URL__` — the
server substitutes the real origin per request — and re-fetch the manifest in Tolgee afterwards.

The page's `icon` is either a **native Tolgee icon name** or an **emoji**; this app uses
`Key01`. Names come from Tolgee's icon registry ([Untitled UI
icons](https://www.untitledui.com/free-icons) plus Tolgee's own set) and must match an exported
component **exactly**, numeric suffix included — `Key01` and `Key02` exist, plain `Key` does
not. Others: `Globe01`, `Translate01`, `LayoutAlt04`, `Settings01`, `BarChart01`, `Zap`.

An unrecognised value is not an error: Tolgee renders the string as literal text. That is what
makes emoji work, and how you spot a typo — the menu shows the word `Key` instead of an icon.

## Layout

```
src/                    iframe page (Vite + React)
  KeysShowcase.tsx      the dashboard page: context, theme, resize
  useProjectKeys.ts     the REST call, via the SDK's typed client
server/
  index.ts              manifest endpoint + optional self-registration
  config.ts             env config and the URLs Tolgee should use
  devTunnel.ts          reads/writes the dev-tunnel state
  manifest.template.json  __BASE_URL__ is substituted at request time
scripts/
  dev-tunnel.ts         opens the tunnel and publishes its URL
  token.ts              machine-to-machine demo
.tolgee-dev/           local state, gitignored
  tunnel.json           the URLs Tolgee currently reaches this app at
  install.json          install id + app credentials, written at registration
```
