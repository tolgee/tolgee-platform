# {{name}}

A Tolgee App scaffolded by `create-tolgee-app`. It contributes one
**project dashboard page**: an iframe Tolgee renders inside a project, with a
menu item of its own.

## Run it

```bash
npm install   # if you skipped it during scaffolding
npm run dev
```

That starts three processes:

| Process | URL | What it serves |
| --- | --- | --- |
| Vite | <http://localhost:{{vitePort}}> | the React app Tolgee embeds |
| Express | <http://localhost:{{serverPort}}> | `GET /manifest.json` |
| Tunnel | printed on start | a public URL for both, when Tolgee is remote |

With Tolgee on localhost the manifest URL is **{{manifestUrl}}** and the tunnel
does nothing.

## Reaching the app from Tolgee

Tolgee fetches the manifest and loads the iframe **from its own server**, not
from your browser. So both have to be reachable from wherever Tolgee runs.

- **Tolgee on localhost** — nothing to do. The tunnel process notices and
  publishes the local URLs unchanged.
- **Tolgee anywhere else** (staging, a preview environment, production) —
  `npm run dev` opens a [Cloudflare quick tunnel](https://developers.cloudflare.com/cloudflare-tunnel/)
  in front of Vite, downloading the `cloudflared` binary on first use. Vite
  proxies `/manifest.json` to Express, so the one public hostname covers the
  manifest and the iframe. A public URL is not optional there: production Tolgee
  runs with `tolgee.apps.allow-local-addresses` disabled and refuses localhost
  manifest URLs.

The tunnel gets a **new hostname on every restart**. The server waits for that
URL before it registers, and self-registration repoints the existing install, so
a restart is all it takes for Tolgee to follow.

`TOLGEE_DEV_TUNNEL=none` in `.env.local` forces the tunnel off and falls back to
`http://localhost:{{vitePort}}`.

The URLs in play live in `.tolgee-dev/tunnel.json` (gitignored), written by the
tunnel process and read by the server on every manifest request.

## Connecting the app to Tolgee

An app becomes usable in two steps: it is **registered** in an organization,
then **enabled** for individual projects.

### 1. Register

Pick whichever fits your setup.

**Manually** — in Tolgee, go to **Organization → Apps**, add an app and give it
the manifest URL the server prints on boot (the tunnel one when tunnelling).
Tolgee fetches the manifest and shows what the app contributes.

**Automatically** — set this in `.env.local`:

```dotenv
TOLGEE_APP_REGISTRATION_SECRET=…
```

The registration secret is server-wide and comes from whoever administers your
Tolgee instance. With it set, the server registers itself on every boot and
logs the result.

This registers a **native** app — one owned by no organization. Which
organizations may use it is a separate, admin-only decision, made in Tolgee
under **Administration → Apps**; a project owner then enables it per project.
(Setting `TOLGEE_ORGANIZATION_SLUG` too installs the app into that single
organization instead, skipping the admin step.) The **first** registration prints a client id and a client
secret — the secret is shown only that once, so paste both into `.env.local`:

```dotenv
TOLGEE_APP_CLIENT_ID=…
TOLGEE_APP_CLIENT_SECRET=…
```

A failed registration never takes the server down; it logs what went wrong and
keeps serving `/manifest.json` so you can fall back to registering by hand.

Registration always uses the URL the tunnel process published, so restarting
`npm run dev` re-registers the app at its new hostname instead of leaving Tolgee
pointed at a dead one.

### 2. Enable for a project

Registration makes the app known to the organization, not visible in projects.
For each project that should use it: **Project → Settings → Apps**, then enable
**{{name}}**. The dashboard page shows up in that project's menu.

## Layout

```
server/manifest.template.json   what the app contributes; __BASE_URL__ is
                                substituted per request
server/index.ts                 Express: /manifest.json + self-registration
server/devTunnel.ts             the URLs Tolgee reaches this app at
scripts/dev-tunnel.ts           opens the tunnel and publishes those URLs
src/App.tsx                     the dashboard page
```

## What the SDK gives you

From `@tolgee/apps-sdk/browser`, inside the iframe:

- **`createTolgeeApp()`** — the postMessage handshake. `await app.context` gives
  you `{ token, apiUrl, organizationId, projectId, theme }`.
- **`createTolgeeAppClient(context)`** — typed REST client with the app's token
  and API URL already wired in. Returns `{ data, error }` rather than throwing.
- **`applyTolgeeTheme(theme)`** — exposes Tolgee's palette as `--tg-color-*` CSS
  variables. Pair it with `app.onThemeChanged` to follow light/dark toggles
  live, and `app.resize(height)` to tell the host how tall the iframe is.

From `@tolgee/apps-sdk/server`:

- **`loadTolgeeAppConfig()`** — reads the environment into a typed config.
- **`renderManifest(template, baseUrl)`** — substitutes `__BASE_URL__`.
- **`tolgeeAppCorsHeaders()`** — CORS headers for endpoints the webapp calls.
- **`selfRegisterApp(…)`** — the boot-time registration used above.
- **`fetchAppAccessToken(…)`** — exchanges the client id/secret for an access
  token, for work the app does on its own behalf rather than a user's.

## Changing what the app contributes

Edit `server/manifest.template.json`. Keep `baseUrl` as `__BASE_URL__` — the
server substitutes the real origin on every request. After changing the
manifest, re-fetch it in Tolgee so the change is picked up.
