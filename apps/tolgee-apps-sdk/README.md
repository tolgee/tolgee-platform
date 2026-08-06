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

### `icon`

Either a **native Tolgee icon name** or an **emoji** (`'🔑'`, `'🧩'`).

Names come from the platform's icon registry — [Untitled UI
icons](https://www.untitledui.com/free-icons) plus Tolgee's own set — and must match an
exported component **exactly**, numeric suffix included:

| Valid | Not valid |
| --- | --- |
| `Key01`, `Key02` | `Key` — no such component |
| `Globe01`, `Translate01`, `LayoutAlt04` | `globe01`, `Globe 01` — case and spacing count |
| `Settings01`, `BarChart01`, `Zap` | `Book01` — not in the set |

An unrecognised value is **not an error**: Tolgee renders the string as literal text.
That is what makes emoji work, and it is how you spot a typo — the menu shows the word
`Key` instead of an icon.

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

### Pin the Tolgee origin

`tolgee-app:init` hands the app an API token, so pass the origin of the Tolgee
instance that is allowed to send it whenever the app knows it — typically baked
in at build time:

```ts
const app = createTolgeeApp({ tolgeeOrigin: 'https://app.tolgee.io' })
```

Anything from another origin is then ignored, and everything the app posts back
is addressed to that origin instead of `*`. Pass an array for an app that runs
against several instances (a tunnel and production, say).

Without it the SDK falls back to trust-on-first-use: the first
`tolgee-app:init` **from the parent window** wins and its origin and window are
pinned for the rest of the session, so no second window can swap the token
afterwards. That does not help against a hostile page that frames the app —
it *is* the parent — which is why declaring the origin is the stronger option.

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

## Server: the auth flows

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
- `secretIssuedAt` records when Tolgee issued the stored secret, which is what
  `ensureAppCredentialsFresh()` ages out.
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

`createTolgeeAppServerClient({ tolgeeUrl, accessToken })` wraps that token in the
same typed REST client the iframe gets, so a backend never hand-writes response
shapes:

```ts
const tolgee = createTolgeeAppServerClient({ tolgeeUrl, accessToken })
const { data, error } = await tolgee.GET('/v2/projects/{projectId}/activity', {
  params: { path: { projectId }, query: { size: 20 } },
})
```

Inside an iframe you don't need this at all: the install token from
`TolgeeAppContext` already authenticates calls as the install + user.

### 3. `rotateAppClientSecret` — replace the secret without anyone copying it

A client secret ends up in the hands of whoever set the app up. When that person
leaves, the organization needs the old credential dead — without deleting the
install, which would take its granted scopes, its availability and every
per-project enablement with it.

Rotation is therefore two deliberate steps, and an install may hold **several
live secrets at once** (up to five):

1. **Issue.** A new secret is minted. Every existing one keeps working.
2. **Revoke.** The old one is invalidated, on the operator's schedule, once
   Tolgee's `lastUsedAt` shows nothing is using it any more.

Step one is the app's own job, and needs no human:

```ts
import { rotateAppClientSecret } from '@tolgee/apps-sdk/server'

await rotateAppClientSecret()
```

The call authenticates with the secret the app already holds, asks Tolgee for a
new one, and writes it to the state file in place of the old one — atomically,
and **never returned and never logged**. The previous secret still
authenticates, so a failed write leaves the app running on what it had.

`ensureAppCredentialsFresh()` is the same thing on a timer, meant for boot:

```ts
await selfRegisterApp({ ... })
await ensureAppCredentialsFresh()   // rotates only if the stored secret is > 30 days old
```

Pass `{ maxAgeMs }` to change the age. It reports rather than throws when there
is nothing to do, and it is a **no-op when the credentials come from
`TOLGEE_APP_CLIENT_ID` / `TOLGEE_APP_CLIENT_SECRET`** — those win over the state
file, so rotating would store a secret the app would never read. Rotate a
deployment by issuing a secret in Tolgee and injecting it.

Run several replicas off one install? Only one of them should rotate: every call
mints another secret, and Tolgee caps how many an install may hold.

Revoking is not the SDK's to do — one replica revoking would cut off its
siblings — so step two happens in Tolgee, under **Organization → Apps** (or
**Administration → Apps** for a native app). An app that genuinely owns its own
lifecycle can still call `DELETE /v2/apps/self/secrets/{id}`; Tolgee refuses to
let it revoke its own last live secret, which would lock it out permanently.

> **A leaked secret is recovered from per install.** There is no publisher
> identity behind a distributed app — every install has credentials of its own
> and there is nobody to authenticate as across all of them. The recourse for a
> mass leak is to rotate each install, which is what the self-service endpoints
> above exist to make scriptable.

### 4. `fetchAppInstallations` — what am I installed for?

An app backend with no iframe and no user has no idea which projects it may
touch: an org admin makes the app available, a project owner enables it, and
either can undo that at any time. `fetchAppInstallations()` asks Tolgee, using
the same credentials as `fetchAppAccessToken()`:

```ts
import { fetchAppInstallations } from '@tolgee/apps-sdk/server'

for (const install of await fetchAppInstallations()) {
  for (const project of install.enabledProjects) {
    console.log(`${project.organization.slug}/${project.name} (#${project.id})`)
  }
}
```

Each `AppInstallation` carries `id`, `appId`, `name`, `version`, `native`
(true when the install belongs to no organization), the `scopes` granted at
consent time, and `enabledProjects` — each with its owning `organization`
(`id`, `name`, `slug`), so a multi-tenant app can partition its work.

`enabledProjects` is the app's authoritative list of what it may act on, and it
changes without the app being told: **re-read it periodically** rather than
caching it for the process lifetime. This alpha has no push channel, so polling
is the only option.

Pass `{ accessToken }` to reuse a token you already hold instead of exchanging
the credentials again.

Only an **install-context** token (the one the client-credentials grant issues)
reaches this endpoint. The user-context token an iframe receives is refused: it
acts for one signed-in user, who need not be a member of every project the
install is enabled for — and the iframe is already told its project and
organization in the init payload.

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

## Developing this package

The SDK lives in the `apps/` npm workspace next to `create-tolgee-app` and the
example apps — a workspace of its own, so the repo root's release-only install
does not drag app dependencies into the server release jobs.

```bash
cd apps
npm install
npm run check      # build + typecheck + test all three packages (what CI runs)
```

```bash
npm run build --workspace @tolgee/apps-sdk    # dist/, what everything else typechecks against
npm run test --workspace @tolgee/apps-sdk
```

## API reference

**`@tolgee/apps-sdk`** — `AppManifest`, `AppModules`, `AppDashboardPage`,
`TolgeeAppContext`, `TolgeeAppTheme`, `AppContextClaims`, `TolgeeApiSchemas`
(Tolgee's generated response shapes, e.g.
`TolgeeApiSchemas['ProjectActivityModel']`; re-exported from all three entry
points).

**`@tolgee/apps-sdk/browser`** — `createTolgeeApp(options?)`, `TolgeeApp`
(`context`, `onThemeChanged`, `resize`, `dispose`), `TolgeeAppOptions`,
`createTolgeeAppClient()`, `TolgeeAppClient`, `applyTolgeeTheme()`.

**`@tolgee/apps-sdk/server`** — `renderManifest()`, `tolgeeAppCorsHeaders()`,
`decodeContextToken()`, `loadTolgeeAppConfig()`, `selfRegisterApp()`,
`fetchAppAccessToken()`, `rotateAppClientSecret()`,
`ensureAppCredentialsFresh()`, `createTolgeeAppServerClient()`,
`fetchAppInstallations()` (`AppInstallation`,
`AppEnabledProject`, `AppInstallationOrganization`, `AppInstallationsInput`),
`appInstallStatePath()`, `readStoredAppInstall()`, `saveAppInstall()`.
