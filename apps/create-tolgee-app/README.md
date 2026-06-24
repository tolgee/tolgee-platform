# create-tolgee-app

Scaffolds a new Tolgee Apps plugin with a working dev environment in one
command.

> **Status: PoC**. Not yet published. The generated project resolves
> `@tolgee/apps-sdk` via npm workspaces, so today the generator only
> works when invoked inside this repo. Run it under `apps/example-apps/`
> and the SDK symlink will pick up automatically.

## What you get

A project pre-wired with:

- **Vite + React + TypeScript** for iframe modules
- **Express + tsx watch** for the webhook + decorator + manifest server
- **Cloudflare quick tunnel** (`scripts/dev.ts`) that boots on `npm run dev`,
  captures a `*.trycloudflare.com` URL, and PATCHes your Tolgee install
  so iframe loads and webhook deliveries reach the local services
- A `manifest.template.json` whose base URL is substituted at request time —
  no manual editing on every restart
- **`@tolgee/apps-sdk`** wired in for the postMessage handshake, REST
  client, webhook signature verification, and typed event dispatch

## Wizard

`npm run dev` (or once published, `npm create tolgee-app@latest`) walks
you through:

1. **App identifier** — kebab-case slug used as `manifest.id`, the
   target directory name, and the `name` in `package.json`.
2. **Display name** — shown in the Tolgee admin UI.
3. **Modules** — pick any subset of the 10 module types.
   Iframe-bearing modules (`project-dashboard-page`, `translation-tools-panel`,
   `key-edit-tab`, `modal`) get source-file stubs; the rest get manifest
   entries pointing back at those iframes.
4. **Webhook events** — pick from a curated list of common
   `ActivityType` values. The webhook route gets a typed `onWebhook(…)`
   stub per event.
5. **Tolgee URL** — defaults to `https://apps.preview.tolgee.io`.
6. **Install deps** + **git init** prompts.

## After generation

```bash
cd <your-app>
npm install          # if you skipped it
npm run register     # one-time: pick org, register the plugin
npm run dev          # starts vite + express + tunnel, PATCHes manifest URL
```

Subsequent `npm run dev` restarts re-PATCH the install with the new
tunnel URL automatically — no manual reinstall.

## Layout

```
apps/create-tolgee-app/
├── src/
│   ├── index.ts        — entry, runs wizard, orchestrates copy
│   ├── wizard.ts       — @clack/prompts flow
│   ├── copy.ts         — recursive copy with mustache substitution
│   ├── manifest.ts     — synthesizes manifest.template.json
│   └── registry.ts     — module + webhook-event registry
├── template/
│   ├── base/           — files every project gets
│   └── modules/<key>/  — iframe stubs per module type
└── package.json
```

## Local build

```bash
npm run --workspace=create-tolgee-app build
```

Outputs an ESM shebang script at `dist/index.js`.
