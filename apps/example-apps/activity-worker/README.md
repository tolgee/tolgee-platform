# Activity Worker — Tolgee App example

A **backend-driven** Tolgee App. Where
[keys-showcase](../keys-showcase) does everything in the iframe, this app does
almost nothing there: its server runs on its own, with no user and no browser,
and the dashboard page is just a window onto what that server has been doing.

Which makes it the app that actually needs
`GET /v2/apps/self/installations` — the endpoint behind
`fetchAppInstallations()`. An iframe is told its project in the init payload; a
background worker is told nothing, and has to ask.

The loop:

1. On boot the server self-registers with the server-wide
   `TOLGEE_APP_REGISTRATION_TOKEN` and gets machine-to-machine credentials.
2. It asks Tolgee **which projects it is enabled for**
   (`fetchAppInstallations()`), each with the organization owning it.
3. For every one of those projects it polls
   `GET /v2/projects/{projectId}/activity`, picks out the **translation
   changes**, and keeps a recent feed of them in memory.
4. It re-asks Tolgee for its installations periodically, so a project that
   enables the app starts being watched **without a restart** — and one that
   disables it stops.
5. Opening **Activity Worker** in a project shows that project's feed:
   *translation X changed in project Y at Z*.

## This app polls. A lot.

The alpha has **no push channel** — no webhooks, no event stream. Tolgee cannot
tell an app that a translation changed, or that a project just enabled it. So
everything here is a poll, and that is a real cost:

| What | Env var | Default | Cost |
| --- | --- | --- | --- |
| Activity of each watched project | `ACTIVITY_POLL_INTERVAL_MS` | `15000` | 1 request **per watched project** per interval |
| The app's own installations | `INSTALLATIONS_REFRESH_INTERVAL_MS` | `60000` | 1 request per interval |
| Revisions read per poll | `ACTIVITY_PAGE_SIZE` | `20` | page size of the activity request |
| Feed entries kept per project | `ACTIVITY_FEED_SIZE` | `50` | memory only |

With 20 projects at the defaults that is ~81 requests a minute, forever, whether
or not anything changed. The activity endpoint is also **rate limited per user**
on the Tolgee side, and an install-context token acts as the install's author —
so several chatty apps sharing an author will throttle each other. Raise the
intervals in anything but a demo.

The dashboard page adds one more poll: it re-reads the app's own feed every 5
seconds (`REFRESH_INTERVAL_MS` in `src/useActivityFeed.ts`), and each of those
asks Tolgee to validate the caller's token — see *Who may read the feed* below.

## What it asks for, and who consents

The manifest requests `activity.view` and `translations.view`.

`activity.view` is not a small ask: **project activity is the audit log** — who
changed which translation, when, and what the text was before. An app holding it
sees every edit in every project it is enabled for. That is exactly what this app
does with it, and it is why it is worth being explicit: the organization admin
who makes the app available, and the project owner who enables it, are consenting
to that on their users' behalf. Tolgee shows the requested scopes at both steps.

Grant it to apps you would trust with the audit log, and no others.

## Prerequisites

A Tolgee dev server with apps turned on. In your `application.yaml` (or as env
vars):

```yaml
tolgee:
  apps:
    enabled: true
    # Needed only because this app runs on localhost. Production instances keep
    # this off, which is why a remote Tolgee needs the dev tunnel below.
    allow-local-addresses: true
```

This app is backend-driven — it needs credentials to poll with, so
self-registration is the practical way to run it. The registration secret comes
from the Tolgee administrator — the server config holds only its hash
(`tolgee.apps.registration-secret-hash`); a dev server sets a known one. The
app registers into `TOLGEE_APP_ORGANIZATION`, or the server's initial
organization when unset.

The app assumes Tolgee is at `http://localhost:8718`; override with `TOLGEE_URL`.

## Install and run

```bash
npm install --prefix ../..   # from apps/ — this is an npm workspace of it
cp .env.example .env.local
npm run dev                  # vite on :5182 + app server on :5183 + dev tunnel
```

Ports are 5182/5183 rather than keys-showcase's 5180/5181, so both example apps
can run at once.

- `http://localhost:5182` — the iframe page (Vite)
- `http://localhost:5183/manifest.json` — the manifest Tolgee fetches
- `http://localhost:5183/api/feed` — the worker's feed (needs a context token)

With Tolgee on the same machine no tunnel is needed: it fetches `localhost`
directly, and the tunnel process says so and stays out of the way.

## Reaching the app from a remote Tolgee

Tolgee fetches the manifest and loads the iframe **from its own server**, so
pointing `TOLGEE_URL` at staging, a preview environment or production means
localhost URLs are useless to it — and those instances run with
`tolgee.apps.allow-local-addresses` disabled, so they reject them outright.

For a non-localhost `TOLGEE_URL`, `npm run dev` opens a
[Cloudflare quick tunnel](https://developers.cloudflare.com/cloudflare-tunnel/)
in front of Vite (downloading the `cloudflared` binary on first use) and
publishes the public URLs to `.tolgee-dev/tunnel.json`. Vite proxies
`/manifest.json` and `/api` to the app server, so one public hostname covers the
manifest, the iframe and the feed API.

The hostname changes on every restart. The server waits for it before
registering, and registration repoints the existing install, so restarting
`npm run dev` is all it takes for Tolgee to follow along.

`TOLGEE_DEV_TUNNEL=none` in `.env.local` forces the tunnel off and falls back to
`http://localhost:5182`.

`cloudflared` is a **dev dependency** — a local-development convenience, so
nothing that deploys this app pulls its binary down. Without it installed the
tunnel process says so and leaves Vite and the app server running.

## Connecting the app to Tolgee

Set the registration token and the server self-registers on boot:

```bash
cat >> .env.local <<'EOF'
TOLGEE_APP_REGISTRATION_TOKEN=<the server's registration secret>
EOF

npm run dev
```

The app is registered into — and owned by — the token's organization; enable it
for a project under that project's **Apps** settings. A server admin can later
offer it to **every** organization from the owner's Apps page, after which any
organization installs it from its own **Available on this server** list.

On the **first** registration Tolgee issues the app's credentials. It shows the
client secret once and stores only its hash, so the SDK writes the install record
straight to `.tolgee-dev/install.json` (gitignored) instead of printing it:

```
Auto-connect: registered install 12 on http://localhost:8718.
  Its credentials are stored in .../activity-worker/.tolgee-dev/install.json (gitignored) — nothing to copy.
```

On later boots the app is already registered, so the server only repoints the
existing install at the current manifest URL — which is what makes a fresh tunnel
hostname take effect; the stored secret is left untouched.

Without a registration token the server still serves the manifest and prints
how to register the app by hand under **Organization → Apps**. The worker will
have no credentials until that happens, and says so.

Set `TOLGEE_APP_CLIENT_ID` and `TOLGEE_APP_CLIENT_SECRET` only when you deploy
the app somewhere that injects secrets properly — the environment wins over the
local file, and setting either one makes the SDK ignore the file entirely.

## Credentials Tolgee pushes at you

Registration is not the only way this app gets credentials. Tolgee POSTs **signed
lifecycle deliveries** to the `baseUrl` in the manifest, and `server/index.ts`
receives them in a single `mountTolgeeLifecycle(app, …)` call.

There are two credential layers, and the deliveries carry both: **app-level**
(`tgpub_` / `tgpubs_`, at registration) identifies and administers the app
everywhere it is installed and reaches no data; **per-install** (`tgapp_` /
`tgapps_`, at install) is what the worker actually polls Tolgee with. A rotation
of either arrives the same way — and this app drops its cached access token when
one does, so the next poll authenticates with the new secret.

A third secret, the **webhook secret**, arrives with the registration and is
never sent anywhere: Tolgee signs each delivery
`HMAC-SHA256(webhookSecret, "<timestamp>.<body>")` in a `Tolgee-Signature`
header, so holding it is what proves a delivery is really Tolgee. The SDK
verifies every one, refuses a stale or replayed timestamp (5-minute window), and
**refuses a first delivery once this app already holds credentials for that
instance**, so nobody can push their own credentials over yours.

Note what this channel does *not* replace: per-project enablement is still
polled, which is what `fetchAppInstallations()` is for.

## Enabling the app for a project

**Project → Settings → Apps → Activity Worker → enable**

Within one `INSTALLATIONS_REFRESH_INTERVAL_MS` the worker picks the project up
and starts polling its activity; the **Activity Worker** item appears in the
project's dashboard menu straight away.

Edit a translation in that project and it shows up in the feed after at most one
`ACTIVITY_POLL_INTERVAL_MS`.

## Who may read the feed

The feed lives in the app's own memory, not in Tolgee, so Tolgee's permissions
do not protect it by themselves. `GET /api/feed` takes the iframe's context token
as a bearer token and **replays it against Tolgee** (`…/activity?size=1`) before
answering. A token Tolgee will not accept for that project does not get the app's
copy of it either.

The token's claims decide nothing on their own: `decodeContextToken` reads the
project id out of the JWT without verifying the signature, so it is only used to
know *which* project to ask Tolgee about.

## Layout

```
src/                    iframe page (Vite + React)
  ActivityWorker.tsx    the dashboard page: context, theme, resize, the feed
  useActivityFeed.ts    polls the app's own /api/feed
  feedTypes.ts          the feed contract, shared with the server
server/
  index.ts              manifest + /api/feed + self-registration + lifecycle + worker start
  activityWorker.ts     the two polling loops (installations, activity)
  translationChanges.ts pulls translation edits out of an activity revision
  tolgeeAccess.ts       install-context token, cached until it nears expiry
  feedRoute.ts          authorizes a feed request against Tolgee
  config.ts             env config and the URLs Tolgee should use
  devTunnel.ts          reads/writes the dev-tunnel state
  manifest.template.json  __BASE_URL__ is substituted at request time
scripts/
  dev-tunnel.ts         opens the tunnel and publishes its URL
.tolgee-dev/            local state, gitignored
  tunnel.json           the URLs Tolgee currently reaches this app at
  install.json          app-level + per-install credentials, written as they arrive
```

## Limits worth knowing

- **In memory only.** Restarting the server empties every feed; it refills from
  the next poll, which only reaches back `ACTIVITY_PAGE_SIZE` revisions.
- **Newest page only.** A burst of more than `ACTIVITY_PAGE_SIZE` revisions
  between two polls loses the oldest of them. A real worker would page back until
  it met a revision id it had already stored.
- **Translation text only.** State changes, comments, tags and key renames are in
  the same activity stream and are ignored here.
