# @tolgee/apps-sdk

SDK for building **Tolgee Apps** — third-party apps that Tolgee embeds in its
webapp and that call the Tolgee REST API back.

> **Alpha.** The public API is not stable yet and may change between alpha
> releases.

## Scope of this alpha

A Tolgee App contributes **modules** — surfaces the platform renders. This alpha
supports exactly one module type:

| Module | What it does |
| --- | --- |
| `project-dashboard-page` | An iframe page under a project, with its own menu item |

No webhooks, no row decorators, no translation-cell selection. Those surfaces
will be added in later releases.

## Install

```bash
npm install @tolgee/apps-sdk
```

Three entry points:

| Import | Use in |
| --- | --- |
| `@tolgee/apps-sdk` | Anywhere — types only |
| `@tolgee/apps-sdk/browser` | The iframe (browser bundle) |
| `@tolgee/apps-sdk/server` | The app's Node backend |

## The manifest

Tolgee fetches a manifest from your app and reads the modules out of it.

```ts
import type { AppManifest } from '@tolgee/apps-sdk'

const manifest: AppManifest = {
  id: 'my-company.glossary',
  name: 'Glossary',
  version: '1.0.0',
  baseUrl: 'https://glossary.example.com',
  scopes: ['keys.view'],
  modules: {
    'project-dashboard-page': [
      {
        key: 'glossary',
        title: 'Glossary',
        icon: 'LayoutAlt04',
        entry: '/dashboard',
      },
    ],
  },
}
```

`icon` is either a name from the platform icon set or an emoji (`'🔑'`). A name has to
match an exported icon component exactly — `Key01`, not `Key`. Anything the platform
does not recognise is rendered as literal text, so a typo shows up as the name itself.

Ship the manifest as a template with a `__BASE_URL__` placeholder and render it
per request, so the URL can change between dev restarts (tunnels) without
editing the file:

```ts
import { renderManifest, tolgeeAppCorsHeaders } from '@tolgee/apps-sdk/server'

app.get('/manifest.json', (req, res) => {
  for (const [k, v] of Object.entries(tolgeeAppCorsHeaders())) res.setHeader(k, v)
  res.type('json').send(renderManifest(template, baseUrl))
})
```

## Browser: the iframe handshake

The host and the iframe talk over `postMessage`:

1. The iframe posts `tolgee-app:ready` as soon as it loads.
2. The host replies with `tolgee-app:init`, carrying the context — an install
   token, the API URL, the project (and organization) ids, and the current theme.
3. The host posts `tolgee-app:theme-changed` whenever the user toggles
   light/dark.
4. The iframe posts `tolgee-app:resize` to ask for a given height.

`createTolgeeApp()` does all of that for you. It sends `ready` on the next
microtask, so you can register handlers synchronously right after constructing
it and still receive the initial values.

```ts
import {
  createTolgeeApp,
  createTolgeeAppClient,
  applyTolgeeTheme,
} from '@tolgee/apps-sdk/browser'

const app = createTolgeeApp()

// Follow the host theme (fires once with the initial theme, then on every toggle).
app.onThemeChanged(applyTolgeeTheme)

const ctx = await app.context

// Typed REST client, pre-authenticated with the install token.
const tolgee = createTolgeeAppClient(ctx)
const { data, error } = await tolgee.GET('/v2/projects/{projectId}', {
  params: { path: { projectId: ctx.projectId } },
})

app.resize(document.body.scrollHeight)
```

`applyTolgeeTheme` writes each palette color as a `--tg-color-*` CSS custom
property (`--tg-color-background`, `--tg-color-text-secondary`, …), sets
`[data-tg-theme="light|dark"]` and `color-scheme` on the root element — so your
styles can be plain CSS:

```css
body {
  background: var(--tg-color-background);
  color: var(--tg-color-text);
}
```

Call `app.dispose()` when your UI unmounts.

## Server: the two auth flows

### 1. `selfRegisterApp` — register without the UI

Instead of pasting a manifest URL into Tolgee's admin UI, an app can register
itself on startup — no restart of Tolgee, no clicking. This is what a dev app
does when its tunnel URL changes on every restart, and how a first-party app
deployed alongside Tolgee connects itself. It requires an instance-wide
registration secret, which the Tolgee admin configures.

```ts
import { loadTolgeeAppConfig, selfRegisterApp } from '@tolgee/apps-sdk/server'

const config = loadTolgeeAppConfig()

const { installId, created, native, credentialsPath } = await selfRegisterApp({
  tolgeeUrl: config.tolgeeUrl,
  registrationSecret: config.registrationSecret!,
  manifestUrl: `${baseUrl}/manifest.json`,
})
```

Omitting `organizationSlug` — the normal case — registers a **native** app: one
owned by no organization. Which organizations may use it is then a server-admin
decision, made in Tolgee under **Administration → Apps**; a project owner
enables it per project afterwards. Pass an `organizationSlug` only when you want
the app installed into that one organization instead.

Tolgee returns the client secret **only when it creates the install** — that's
what `created` reflects. A later call for an already registered app repoints it
at the new manifest URL and returns `clientSecret: null`, leaving the existing
credentials valid.

### Where the credentials go

Tolgee shows the client secret once and stores only its hash, so the SDK writes
the whole install record — install id, client id, client secret, and the
`tolgeeUrl` it belongs to — to a local state file as soon as registration
returns it. `credentialsPath` in the result is where it landed:

```
.tolgee-dev/install.json      # gitignored; mode 0600
```

Nothing to copy, and **never print the secret**. Log `credentialsPath` instead.

- The directory is `.tolgee-dev` under the working directory, or
  `TOLGEE_APP_STATE_DIR`, or the `stateDir` option — `appInstallStatePath()`
  resolves the same path the SDK uses.
- Records are keyed by Tolgee instance, so credentials issued by one instance
  are never handed to another.
- A re-registration that returns `clientSecret: null` keeps the stored secret.
- Writes go through a temp file and a rename, so an interrupted or concurrent
  write cannot leave a half-written file behind; an unreadable file reads as
  "nothing stored" rather than throwing.
- `persist: false` opts out, for apps that capture the secret themselves.

`readStoredAppInstall(tolgeeUrl)` and `saveAppInstall(record)` are exported for
apps that manage the record themselves.

### 2. `fetchAppAccessToken` — act as the app itself

For work outside any iframe (background jobs, cron, webhooks of your own), an
app backend authenticates with the OAuth 2.0 client-credentials grant. With
nothing passed it uses the credentials `loadTolgeeAppConfig()` resolves, so a
registered app needs no wiring at all:

```ts
import { fetchAppAccessToken } from '@tolgee/apps-sdk/server'

const { accessToken, expiresIn } = await fetchAppAccessToken()
```

Pass `{ tolgeeUrl, clientId, clientSecret }` to override any of them. With no
credentials anywhere it throws, naming both the env vars and the state file.

The access token is **short-lived**: don't cache it past `expiresIn` — re-fetch
when it expires (or on a `401`). The **client secret must only ever be sent to
this endpoint** — never to the browser, and never as a bearer token on API calls.

Inside an iframe you don't need this at all: the install token from
`TolgeeAppContext` already authenticates calls as the install + user.

### Reading the context token

`decodeContextToken` pulls the ids out of the install JWT for logging, routing,
or persistence. It **decodes without verifying** the signature — apps don't hold
Tolgee's signing key. Verification happens at Tolgee when you pass the token as
a bearer token, so never make a trust decision on the decoded claims alone.

```ts
import { decodeContextToken } from '@tolgee/apps-sdk/server'

const { installId, projectId, userId, expiresAt } = decodeContextToken(token)
```

### Configuration

`loadTolgeeAppConfig()` reads the standard env-var contract:

| Env var | Config field | Default |
| --- | --- | --- |
| `TOLGEE_URL` | `tolgeeUrl` | `http://localhost:8718` |
| `VITE_PORT` | `vitePort` | `5180` |
| `SERVER_PORT` (or `PORT`) | `serverPort` | `5181` |
| `TOLGEE_ORGANIZATION_SLUG` | `organizationSlug` | `null` |
| `TOLGEE_APP_REGISTRATION_SECRET` | `registrationSecret` | `null` |
| `TOLGEE_APP_CLIENT_ID` | `clientId` | stored credentials |
| `TOLGEE_APP_CLIENT_SECRET` | `clientSecret` | stored credentials |
| `TOLGEE_APP_STATE_DIR` | — | `.tolgee-dev` in the working directory |

Credentials fall back to the install record stored for the same `tolgeeUrl`;
`credentialsSource` says which won (`'env'`, `'stored'` or `null`), and
`installId` is the stored install.

**The environment always wins.** A deployed app gets its secrets injected and
must not be overridden by a state file left behind by a developer. Setting
either `TOLGEE_APP_CLIENT_ID` or `TOLGEE_APP_CLIENT_SECRET` makes the SDK ignore
the stored record completely — an env client id is never paired with a stored
secret — so in a deployment set both.

## API reference

**`@tolgee/apps-sdk`** — `AppManifest`, `AppModules`, `AppDashboardPage`,
`TolgeeAppContext`, `TolgeeAppTheme`, `AppContextClaims`.

**`@tolgee/apps-sdk/browser`** — `createTolgeeApp()`, `TolgeeApp`
(`context`, `onThemeChanged`, `resize`, `dispose`), `createTolgeeAppClient()`,
`applyTolgeeTheme()`.

**`@tolgee/apps-sdk/server`** — `renderManifest()`, `tolgeeAppCorsHeaders()`,
`decodeContextToken()`, `loadTolgeeAppConfig()`, `selfRegisterApp()`,
`fetchAppAccessToken()`, `appInstallStatePath()`, `readStoredAppInstall()`,
`saveAppInstall()`.
