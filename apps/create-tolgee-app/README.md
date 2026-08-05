# create-tolgee-app

Scaffolds a working Tolgee App in one command: a Vite + React iframe page, a
small Express server that publishes the app manifest, and a `.env.local` wired
to your Tolgee instance.

```bash
npx create-tolgee-app my-app
```

> **Status: alpha, needs a checkout.** Inside this repository the package is
> resolved through npm workspaces, so `npm install` in `apps/` links it (and
> `@tolgee/apps-sdk`) locally. `npx create-tolgee-app` from a clean machine
> **exits with an error**: no `@tolgee/apps-sdk` on npm carries the API the
> template imports, so the scaffold could only produce a project that does not
> compile. Run from a checkout, it scaffolds anywhere on disk and wires the
> generated app to the SDK sources — see *Which SDK the generated app gets*.

## What it generates

A standalone project that contributes exactly one **project dashboard page** —
the only module type the platform supports in this alpha. No webhooks, no
decorators, no actions, modals, panels or shortcuts; those are not in the
platform yet, so the generator does not offer them.

```
my-app/
├── server/
│   ├── manifest.template.json   what the app contributes to Tolgee
│   ├── index.ts                 Express: GET /manifest.json
│   ├── devTunnel.ts             the URLs Tolgee reaches the app at
│   └── register.ts              optional self-registration on boot
├── scripts/
│   └── dev-tunnel.ts            Cloudflare quick tunnel for remote Tolgee
├── src/
│   ├── App.tsx                  the dashboard page
│   └── App.css                  styled with Tolgee's --tg-color-* variables
├── .env.local                   filled in from your answers (gitignored)
└── README.md
```

The generated page is not a placeholder: it completes the postMessage
handshake, applies the host theme and follows light/dark changes, reports its
height back to Tolgee, and makes a real REST call (fetching the current
project) so you can see the app's token working before writing any code.

## Wizard

```
App id                kebab-case; the folder name, package name and manifest id
Display name          shown in Tolgee's UI
Tolgee URL            default http://localhost:8718
How to register       manual | self-register  (see below)
  Organization slug   self-register only
  Registration secret self-register only
Install dependencies? y/n
Initialize git?       y/n
```

## Connect modes

An app has to be **registered** in an organization before a project can enable
it. The wizard asks how you want that to happen.

**`manual`** — you register the app yourself in Tolgee's UI, under
**Organization → Apps**, using the manifest URL the CLI prints
(`http://localhost:5181/manifest.json` by default).

**`auto`** — the generated server registers itself on boot, using the
server-wide registration secret. The wizard also asks for the organization slug
and the secret, and writes both into `.env.local`. Tolgee shows the client
secret only at that first registration, so the SDK stores the install record in
`.tolgee-dev/install.json` (gitignored) and reads it back later — nothing to
copy, and the secret is never printed. If registration fails, the server logs an
actionable error and keeps serving the manifest, so manual registration remains
available.

Either way, the last step is the same and is done per project:
**Project → Settings → Apps**, enable the app.

## Which SDK the generated app gets

The generated app depends on `@tolgee/apps-sdk`, and by default the generator
points it at the SDK **of this checkout** rather than the registry — the
published package trails the sources by whole APIs, and the SDK's npm `latest`
tag is older still, so a `*` range produces an app that does not compile.

`--sdk=<mode>` overrides the choice; the resolved one is shown in the wizard
summary and again in the next steps.

| Mode | What the dependency becomes |
| --- | --- |
| `auto` (default) | `*` when the app is generated into the `apps/` npm workspaces (they link the SDK already), otherwise `file:<abs path>/apps/tolgee-apps-sdk` |
| `local` | as `auto`, but fails instead of falling back to the registry |
| `published` | an exact published version, never a range |

**`published` currently refuses to run.** Every `@tolgee/apps-sdk` release on npm
predates `selfRegisterApp` and `applyTolgeeTheme`, which the template imports, so
the generator exits with that sentence rather than handing you a project full of
missing-export errors. `PUBLISHED_SDK_RELEASED` in `src/registry.ts` is the flag
to flip — together with `PUBLISHED_SDK_VERSION` — once the SDK is on npm. `auto`
hits the same wall only when the CLI has no SDK sources next to it.

A `file:` dependency uses the SDK's build output, so the SDK has to be built:

```bash
npm run build --workspace @tolgee/apps-sdk   # from apps/
```

The generator does not build it for you, but it does check and says so in the
next steps when `dist/` is missing.

## Dev tunnel

The generated app talks to Tolgee, and Tolgee has to fetch the manifest and load
the iframe from its own server. `npm run dev` therefore runs a third process
alongside Vite and Express: with a non-localhost `TOLGEE_URL` it opens a
Cloudflare quick tunnel, publishes the public URLs to `.tolgee-dev/tunnel.json`,
and the server registers the app with *those* URLs. `TOLGEE_DEV_TUNNEL=none`, or
a localhost `TOLGEE_URL`, keeps everything local.

`cloudflared` is a **dev dependency** of the generated app — it downloads a
platform binary on install, which no deployment of the app needs. A production
install (`npm ci --omit=dev`) simply has no tunnel; the script says so and lets
Vite and the app server carry on.

## Non-interactive use

```bash
create-tolgee-app my-app --yes \
  --name="My App" \
  --tolgee-url=http://localhost:8718 \
  --connect=auto --org=my-org --secret=… \
  --sdk=auto --install --git
```

`--yes` (or `-y`) skips every prompt. `--connect` defaults to `manual`;
`--org` and `--secret` are required with `--connect=auto`. `--install` and
`--git` are opt-in flags in this mode. `--sdk` works in both modes.

## Ports

Vite serves the app on `5180`, Express serves the manifest on `5181`. Both are
read from `VITE_PORT` / `SERVER_PORT` in the generated `.env.local` — change
them there to run two apps side by side.

## Developing this package

It is part of the `apps/` npm workspace, so dependencies come from
`npm install` there, not at the repo root.

```bash
npm run dev        # run the CLI from source (tsx)
npm run typecheck
npm run build      # bundle to dist/ with tsup
```

`template/` ships as-is inside the published package. Files there use
`{{mustache}}` placeholders, and a leading underscore stands in for a dot
(`_gitignore` → `.gitignore`, `_env.example` → `.env.example`,
`_package.json` → `package.json`) so npm does not treat the template as a real
package.
