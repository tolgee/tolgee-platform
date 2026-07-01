# Back Translate

A Tolgee Apps plugin scaffolded by `create-tolgee-app`.

## Quick start

```bash
npm install           # if you skipped it during scaffolding
npm run register      # one-time: pick your org, register the plugin
npm run dev           # boots Vite (5180) + Express (5181) + Cloudflare tunnel
```

The first `npm run dev` after registration PATCHes the install in Tolgee
to point at the fresh tunnel URL. Restart `dev` as many times as you like —
each restart re-PATCHes; no manual reinstall.

## Layout

```
manifest.template.json   ← edit this; baseUrl is __BASE_URL__ at request time
src/                     ← iframe modules (one folder per `entry`)
server/                  ← webhook + decorator endpoints
scripts/                 ← dev orchestrator + one-time register
```

## What the SDK gives you

- **`createTolgeeApp()`** in iframes — postMessage handshake, JWT
  context, selection updates.
- **`createTolgeeAppClient(ctx)`** — typed REST client to Tolgee with
  the install token pre-attached.
- **`verifyWebhookSignature({ header, rawBody, secret })`** — pure
  WebCrypto-based HMAC check matching Tolgee's `Tolgee-Signature` header.
- **`onWebhook(payload, 'EVENT_NAME', handler)`** — typed dispatcher
  that narrows `payload.activityData` by event type.
- **`decodeContextToken(jwt)`** — extract install/project/user ids
  from the JWT on the server.

## Editing the manifest

`server/manifest.template.json` is the source of truth. The server reads
it on every `/manifest.json` request and substitutes `__BASE_URL__` with
the live tunnel URL. After saving edits, run `npx tsx scripts/refresh.ts`
(or restart `npm run dev`) to have Tolgee re-fetch.
