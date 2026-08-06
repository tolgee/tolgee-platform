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

No webhook modules, no row decorators, no translation-cell selection. Those
surfaces will be added in later releases. (Tolgee does push **lifecycle**
deliveries to every app — credentials and installs; see
[the lifecycle channel](#5-the-lifecycle-channel).)

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

### The two credential layers

An app has credentials at two levels, and they are not interchangeable:

| Layer | Prefixes | Issued when | What it is for |
| --- | --- | --- | --- |
| **App** | `tgpub_` / `tgpubs_` | the app is registered, once server-wide | Identifying and administering the app across every organization that installed it. **Grants access to no data at all.** |
| **Install** | `tgapp_` / `tgapps_` | each organization installs the app | Acting on that one organization's projects. This is what `fetchAppAccessToken()` uses. |

Alongside them Tolgee issues a third secret, the **webhook secret**. It is not a
credential you send anywhere — it is the key Tolgee signs its deliveries to your
app with, so holding it is what lets you tell a real delivery from a forged one.
See [the lifecycle channel](#5-the-lifecycle-channel).

### Where the credentials go

Tolgee shows each secret once and stores only its hash, so the SDK writes what
it is given to a local state file as soon as it arrives.
`credentialsPath` in a registration result is where it landed:

```
.tolgee-dev/install.json      # gitignored; mode 0600
```

Nothing to copy, and **never print a secret**. Log `credentialsPath` instead.

The file holds both layers, per Tolgee instance:

```jsonc
{
  "version": 2,
  "instances": {
    "https://app.tolgee.io": {
      "app": { "appId": "my-company.glossary", "clientId": "tgpub_…", … },
      "currentInstallId": 7,
      "installs": {
        "7": { "installId": 7, "clientId": "tgapp_…", "organizationSlug": "acme", … }
      }
    }
  }
}
```

- The directory is `.tolgee-dev` under the working directory, or
  `TOLGEE_APP_STATE_DIR`, or the `stateDir` option — `appInstallStatePath()`
  resolves the same path the SDK uses.
- Records are keyed by Tolgee instance, so credentials issued by one instance
  are never handed to another.
- One app can be installed by many organizations, so installs are keyed by
  install id. `readStoredAppInstall()` returns the one this app authenticates
  as — the install it registered itself, or the first one it was told about —
  and `readStoredAppInstalls()` returns all of them.
- A file written by an earlier SDK (`version: 1`, one install per URL, no app
  layer) is **read forward**, not discarded: its install becomes the current one
  and nothing has to be registered again.
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

> **App-level rotation is separate.** The calls above rotate the credentials of
> **one install**. The app-level secret (`tgpubs_…`) is rotated by its owning
> organization in Tolgee, and the new one arrives over
> [the lifecycle channel](#5-the-lifecycle-channel) — nothing to copy there
> either.

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
caching it for the process lifetime. The lifecycle channel below tells you about
installs and uninstalls, but per-project enablement is not pushed — poll for
that.

Pass `{ accessToken }` to reuse a token you already hold instead of exchanging
the credentials again.

Only an **install-context** token (the one the client-credentials grant issues)
reaches this endpoint. The user-context token an iframe receives is refused: it
acts for one signed-in user, who need not be a member of every project the
install is enabled for — and the iframe is already told its project and
organization in the init payload.

### 5. The lifecycle channel

Tolgee **pushes** credentials to your app instead of making somebody copy them.
It POSTs a signed delivery to the `baseUrl` in your manifest — that delivery is
what proves you control the app's domain, and it is the only way per-install
credentials ever leave Tolgee.

One call receives the whole channel:

```ts
import { mountTolgeeLifecycle } from '@tolgee/apps-sdk/server'

mountTolgeeLifecycle(app, {
  tolgeeUrl: config.tolgeeUrl,
  on: {
    installed: (event) => console.log(`installed by ${event.organization?.slug}`),
    uninstalled: (event) => console.log(`install ${event.install?.installId} is gone`),
  },
})
```

The SDK verifies the signature, rejects anything that fails, stores the
credentials the delivery carries, and only then calls your listeners — which
never have to touch a secret. Everything in `on` is optional; an app that just
wants its credentials stored passes none at all.

Tolgee addresses the delivery at the `baseUrl` itself, so `mountTolgeeLifecycle`
answers `POST /` — and `POST /tolgee/lifecycle` as well, for an app that routes
deliveries somewhere explicit. Pass `paths` to change that.

**Mount it before any body parser.** The signature covers the exact bytes Tolgee
sent, so the handler reads the raw request itself; `express.json()` mounted
first drains the stream and the delivery is refused with a message saying so.

Not on Express? `createTolgeeLifecycleHandler(options)` returns a plain
`(req, res)` Node handler, and `receiveTolgeeDelivery({ rawBody,
signatureHeader, … })` is the whole receiver with no HTTP in it.

#### What each event carries

| Event | Carries |
| --- | --- |
| `app.registered` | App-level `clientId` / `clientSecret`, the **webhook secret**, the manifest `appId` — plus the registering organization's install |
| `app.installed` | Per-install `clientId` / `clientSecret`, the install id, and the organization (`id`, `name`, `slug`) |
| `app.uninstalled` | The install id that is gone; the SDK drops its stored credentials |
| `app.secret.rotated` | The replacement secret, and `rotatedLayer` — `'app'` or `'install'` — saying which layer it belongs to |

Every event also carries `timestamp`, the `tolgeeUrl` it was accepted for,
`deliveryId` — stable across Tolgee's retries of the same delivery, so it is what
to key on when a listener must run exactly once — and the verified `payload` for
fields the SDK does not model.

#### How a delivery is verified

Tolgee signs the body the same way its outgoing webhooks are signed: a
`Tolgee-Signature` header holding `{"timestamp": …, "signature": "…"}`, where the
signature is `HMAC-SHA256(webhookSecret, "<timestamp>.<body>")` in hex.

**The webhook secret is what proves a delivery is really Tolgee.** Nothing else
does — not the source IP, not the shape of the payload.

- A delivery is **stale** if its timestamp is more than **5 minutes** from your
  clock, in either direction, and is refused. That window is wide enough for
  retry backoff and ordinary clock skew, narrow enough that a delivery captured
  off the wire stops being replayable within minutes. An accepted signature is
  also remembered for that window, so the same delivery replayed inside it is
  refused too.
- **The first delivery is the awkward one.** Tolgee discloses the webhook secret
  in `app.registered` itself, so that one delivery can only be checked against
  the key it brought along — which proves nothing but its own integrity. The SDK
  accepts it **only while the app holds no credentials for that instance**, and
  **refuses it (409) the moment it does**. That is the anti-hijack rule: a
  stranger cannot post a self-signed "you were just registered" and overwrite a
  live install.
- **Every later delivery is genuinely authenticated**, because it is checked
  against the stored webhook secret, which only Tolgee knows. A rotation
  therefore replaces what is held — that is the point of it.
- Set `TOLGEE_APP_WEBHOOK_SECRET` (or pass `webhookSecret`) in a deployment and
  there is no first delivery to trust at all. Add `requireKnownSecret: true` to
  refuse every delivery that cannot be checked against a secret you already
  hold. It is also the way out of a refused first delivery.

Rejections come back as a status and a `rejection` code — `bad-signature`,
`stale-timestamp`, `replayed`, `credentials-already-held`, `unverifiable`,
`unreadable-body`, `unknown-event` — and the message never contains secret
material, so it is safe to log. Pass `onRejected` to see them.

If a listener throws, the delivery is answered `500` and Tolgee retries it;
whatever the delivery carried is already stored by then.

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
| `TOLGEE_APP_WEBHOOK_SECRET` | `webhookSecret` | stored app-level record |
| `TOLGEE_APP_STATE_DIR` | — | `.tolgee-dev` in the working directory |

Credentials fall back to the install record stored for the same `tolgeeUrl`;
`credentialsSource` says which won (`'env'`, `'stored'` or `null`), and
`installId` is the stored install. `appClientId` and `webhookSecret` come from
the app-level record — a separate layer, so the client-credential override below
has no say over them.

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
`appInstallStatePath()`, `readStoredApp()`, `saveApp()`,
`readStoredAppInstall()`, `readStoredAppInstalls()`,
`readStoredAppInstallById()`, `saveAppInstall()`, `forgetAppInstall()`,
`forgetTolgeeInstance()`, `hasStoredCredentials()`.

The lifecycle channel: `mountTolgeeLifecycle()`,
`createTolgeeLifecycleHandler()`, `receiveTolgeeDelivery()`,
`TOLGEE_LIFECYCLE_PATHS`, `verifyTolgeeSignature()`,
`computeTolgeeSignature()`, `parseSignatureHeader()`, `TolgeeSignatureError`,
`TOLGEE_SIGNATURE_HEADER`, `DEFAULT_SIGNATURE_TOLERANCE_MS`, and the types
`TolgeeLifecycleEvent`, `TolgeeLifecycleEventType`, `TolgeeCredentialLayer`,
`DeliveredAppCredentials`, `DeliveredInstall`, `DeliveredOrganization`,
`DeliveryResult`, `DeliveryRejection`, `TolgeeLifecycleListeners`,
`TolgeeLifecycleOptions`.
