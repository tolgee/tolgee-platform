# Tolgee Dev Plugin

A minimal example of a Tolgee App used for local development of the Tolgee Apps
PoC. Serves a manifest, an iframe page, and a small backend that receives
webhooks and stores per-translation emoji + last-updated-at side data.

## Running

```bash
npm install
npm run dev
```

`npm run dev` starts two processes side-by-side via `concurrently`:

- **Vite** on `http://localhost:5180` — serves the manifest at `/manifest.json`,
  the iframe page, and proxies `/api/*` to the backend so the iframe can call
  the plugin's REST API same-origin.
- **Express** on `http://localhost:5181` — receives webhooks directly from
  Tolgee and handles `/api/*` for the iframe (via Vite's proxy).

The manifest's `baseUrl` is `http://localhost:5180` (Vite, where the iframe is
served), but `webhooks.url` is the **absolute** `http://localhost:5181/webhook`
so Tolgee's server-side dispatcher hits Express directly without depending on
Vite being up. In a real deployment, both would live behind a reverse proxy on
one host, so the webhook URL would normally be relative.

## Env vars

| Var | Purpose |
| --- | --- |
| `TOLGEE_WEBHOOK_SECRET` | HMAC key Tolgee signs webhook bodies with. If unset, the server warns and accepts every webhook. Recommended to set in any non-trivial test. |
| `PORT` | Override the backend port (default `5181`). |

Get the webhook secret from the Tolgee "Custom apps" admin row after registering
this plugin. The same row also shows the `clientId` + `clientSecret`, which a
real plugin would use to call Tolgee's REST API on its own behalf — this
example doesn't make outbound API calls, so the secret only matters for webhook
verification here.

## Storage

`server/data.json` (gitignored) holds:

```json
{
  "emojis": { "<translationId>": "🎉" },
  "updatedAt": { "<translationId>": "2026-05-15T12:00:00.000Z" }
}
```

Translation IDs are globally unique in Tolgee, so a single shared file works
across projects.

## Frontend ↔ Backend auth — your problem

Tolgee doesn't authenticate `plugin-frontend → plugin-backend` calls; that's up
to the plugin author. This example leaves `/api/*` wide open because it runs on
localhost. A real plugin would either validate the iframe's user-context JWT
against Tolgee's `/v2/user` endpoint or use its own auth scheme.
