# Tolgee Apps — Architecture Notes

This document describes the architecture of the **Tolgee Apps** plugin system. It is intended as a reference for anyone working on the apps subsystem (manifest handling, iframe rendering, SDK, signed tokens, webhooks) or building a plugin against it.

Status: PoC design. Subject to revision as scopes are built. The PoC scope covers only the **project dashboard page** extension point — REST API client, webhooks, editor extensions, inline annotations, dev CLI and ZIP delivery are planned for later scopes.

---

## 1. Overview

### 1.1 What this is

Tolgee Apps is a plugin system that lets third-party developers extend Tolgee with custom UI, backend behavior, and event reactions. The model borrows the manifest + module pattern from **Crowdin Apps** and the cryptographic auth model from **Atlassian Connect**.

For the PoC, the only capability shipped is: **a plugin can register one or more pages in a project's left navigation**. Clicking such a page opens the plugin's UI in a sandboxed iframe inside the project content area, with the project/user/org identity available to the plugin via a signed context token.

### 1.2 Core model in one paragraph

A plugin is a **web service hosted by its author**, described by a JSON **manifest**. An organization admin registers the plugin against their organization by giving Tolgee the manifest URL. Project admins then enable the registered plugin per-project. When a user navigates to an enabled plugin's page, Tolgee renders an iframe pointing at the plugin's `entry` URL and passes a short-lived **JWT signed by Tolgee** carrying the install / org / project / user identity. The iframe communicates with the Tolgee parent page via `postMessage`. The same JWT is also the credential the plugin uses to call Tolgee's REST API and (when present) the plugin's own backend.

### 1.3 What is and is not in the PoC

| Capability | PoC | Later scope |
| --- | --- | --- |
| Register plugin against organization | ✓ | |
| Enable plugin per project | ✓ | |
| `project-dashboard-page` module → sidebar entry + iframe | ✓ | |
| Signed context token delivered to iframe | ✓ | |
| `@tolgee/apps-sdk` exposing `getContext()` | ✓ | |
| Plugin → Tolgee REST API auth (token-based) | | ✓ |
| Permission scopes + install-time consent | | ✓ |
| Webhook subscriptions | | ✓ |
| Editor extensions, inline translation annotations | | ✓ |
| Local dev CLI + tunnel | | ✓ |
| ZIP-based plugin delivery (Tolgee-served bundles) | | ✓ |

### 1.4 What we deliberately did *not* copy from elsewhere

- **GitHub Apps' "no embedded UI" rule** — Tolgee apps render UI inside the product, GitHub apps do not.
- **Figma's QuickJS-WASM sandbox** — far too much infra for a localization tool. We rely on browser-enforced iframe origin isolation instead.
- **Crowdin's mandatory hosted backend even for static plugins** — the ZIP delivery path (later scope) will let UI-only plugins skip hosting entirely.

---

## 2. End-to-end flow

### 2.1 Plugin author publishes a plugin

The plugin author hosts:

- A `manifest.json` at a stable URL (e.g. `https://my-plugin.example.com/manifest.json`)
- Whatever assets each module's `entry` URL resolves to (HTML, JS, CSS)
- Optionally: a plugin backend at any URL of their choosing (for stateful plugins; not required for pure UI plugins)

### 2.2 Org admin registers the plugin

In the org settings UI:

1. Admin pastes the manifest URL.
2. Tolgee fetches the manifest, validates it, and stores a snapshot keyed by `pluginId` and the registering `orgId`. The result is a record we call an **install**, with its own `installId`.
3. Admin sees the parsed manifest (name, version, declared modules) and confirms registration.

The manifest is re-fetched only on explicit admin action ("refresh"). Tolgee does not periodically poll plugin URLs.

### 2.3 Project admin enables the plugin per project

In project settings:

1. Project admin sees the list of plugins registered in the organization.
2. Admin toggles a plugin on for this project; enablement state is persisted per project.
3. Disabling removes the plugin's presence everywhere in that project.

### 2.4 User navigates to the plugin's page

Per enabled plugin, each `project-dashboard-page` module in the manifest appears as an entry in the project sidebar (using `title` and `icon` from the manifest).

When the user clicks the entry:

1. Tolgee routes to a URL of the shape `app.tolgee.io/projects/<projectId>/plugins/<pluginId>/<moduleKey>`. The slug after `/plugins/<pluginId>/` is derived from the module's `key`.
2. Tolgee mints a JWT (see §3.2) signed with its private key.
3. Tolgee renders a sandboxed iframe whose `src` is the plugin's `baseUrl` + `entry`, with the JWT passed in the URL.
4. The iframe loads the plugin's bundle; the SDK reads the JWT and exposes context via `tolgee.getContext()`.

### 2.5 Plugin calls Tolgee's REST API (later scope)

```ts
fetch(`${tolgeeApiBase}/v2/projects/${ctx.projectId}/translations`, {
  headers: { Authorization: `Bearer ${ctx.token}` },
})
```

Tolgee validates its own signature on the JWT, enforces the install's granted scopes plus the user's project role, and executes the call.

### 2.6 Plugin calls its own backend (later scope)

```ts
fetch('https://my-plugin.example.com/api/save', {
  headers: { Authorization: `Bearer ${ctx.token}` },
})
```

The plugin backend verifies the JWT against Tolgee's published public key (JWKS endpoint) and trusts the claims. No separate login system on the plugin side.

---

## 3. Reference

### 3.1 Manifest schema (PoC)

```json
{
  "id": "tolgee-dev-plugin",
  "name": "Tolgee Dev Plugin",
  "version": "0.1.0",
  "baseUrl": "https://my-plugin.example.com",
  "modules": {
    "project-dashboard-page": [
      {
        "key": "hello",
        "title": "Hello World",
        "icon": "👋",
        "entry": "/"
      }
    ]
  }
}
```

| Field | Type | Description |
| --- | --- | --- |
| `id` | string | Globally unique plugin identifier. |
| `name` | string | Human-readable name; shown in admin UI and sidebar. |
| `version` | string | Plugin version (semver recommended). |
| `baseUrl` | string | Origin where the plugin's assets are served. Module `entry` paths are resolved against this. Must be HTTPS in production. |
| `modules.project-dashboard-page[]` | array | Pages this plugin contributes to the project sidebar. |
| `modules.project-dashboard-page[].key` | string | Unique within the plugin; used as the URL slug under Tolgee's namespace. |
| `modules.project-dashboard-page[].title` | string | Sidebar entry title. |
| `modules.project-dashboard-page[].icon` | string | Sidebar entry icon (emoji or icon-name TBD). |
| `modules.project-dashboard-page[].entry` | string | Path relative to `baseUrl`; the iframe will load this URL. |

Later scopes will extend `modules` with additional types (`editor-right-panel`, `translation-decorator`, `custom-mt`, `modal`, …) and add top-level fields (`scopes`, `webhooks`, `backendBaseUrl`, optionally `assets` when ZIP delivery lands).

### 3.2 Context token (JWT) schema

```jsonc
{
  "iss": "tolgee.io",         // issuer
  "installId": "...",         // org's installation of this plugin
  "pluginId": "...",
  "orgId": "...",
  "projectId": "...",
  "userId": "...",
  "locale": "en",             // user's UI locale
  "scopes": [],               // granted permission scopes (empty in PoC)
  "exp": 1736000000           // short TTL (~1h planned)
}
```

Signed by Tolgee's private key. For the PoC the token is delivered as a query string parameter on the iframe URL. In a later scope it can be delivered via `postMessage` instead, to avoid the token leaking through `Referer` headers or browser history.

Plugin backends verify the JWT using Tolgee's public key, published at a well-known JWKS endpoint (`/.well-known/jwks.json`). Tolgee verifies its own signature on incoming REST API calls.

### 3.3 URL spaces

Two URL spaces meet:

| Field | Whose URL | Example | What it controls |
| --- | --- | --- | --- |
| Tolgee-side URL slug | Tolgee | `app.tolgee.io/projects/123/plugins/tolgee-dev-plugin/hello` | Where the plugin page mounts inside Tolgee — needed for deep links, browser history, multi-page disambiguation. Slug derived from `pluginId` + module `key`. |
| `entry` | Plugin | `${baseUrl}/` | What Tolgee fetches into the iframe. |

For the PoC, the manifest only specifies `entry`. Tolgee derives its in-product slug from the plugin id + module key — no explicit `path` field is required.

### 3.4 postMessage protocol

The iframe and the Tolgee parent page communicate via `window.postMessage` only. Same-origin policy guarantees the iframe cannot read or write the parent's DOM, cookies, or storage directly.

The SDK wraps the raw API to provide an async request/response style:

```ts
const ctx = await tolgee.getContext()
```

Under the hood:

1. SDK generates a UUID correlation id.
2. Sends `{ type, id, params }` via `window.parent.postMessage(..., TOLGEE_ORIGIN)`.
3. Listens for a reply with matching `replyTo: id`.
4. Resolves the Promise with the reply's `result`.

The Tolgee webapp runs a dispatcher that routes `type` to a handler (`getContext`, `refreshToken`, `apiCall`, …) and posts the result back.

**Security rules — applied on both sides:**

- When Tolgee posts to the iframe: `targetOrigin` must be the plugin's exact origin. Never `*`.
- When the iframe receives a message: `event.origin` must match the Tolgee origin. Reject otherwise.
- The SDK enforces both. Plugin authors never see the raw API.

Hand-rolling the correlation-id plumbing is fine for the PoC. For later polish, [`comlink`](https://github.com/GoogleChromeLabs/comlink) and [`penpal`](https://github.com/Aaronius/penpal) wrap postMessage into proxy-feeling RPC APIs and are worth evaluating before scaling the SDK surface.

### 3.5 Iframe sandboxing

The iframe must run in an origin distinct from Tolgee's webapp so the browser blocks DOM/cookie/storage access. Two viable approaches:

| Approach | How | Trade-off |
| --- | --- | --- |
| **Per-plugin subdomain** | Serve / proxy plugin content under `plugin-<id>.plugins.tolgee.io` | Cleanest origin isolation; requires wildcard DNS + cert; needed if plugins ever want their own cookies/localStorage |
| **`<iframe sandbox>` with null origin** | Use `<iframe sandbox="allow-scripts">` (deliberately *without* `allow-same-origin`) | No DNS work; browser assigns opaque null origin; iframe cannot use its own cookies/localStorage |

PoC default: `<iframe sandbox>`. Per-plugin subdomain becomes the upgrade path when plugins start needing persistent state on their own origin.

### 3.6 Multi-tenancy

A plugin is deployed once at one URL but serves many tenants. The frontend bundle is identical for all installs; the **context token** carries per-load identity (`installId`, `orgId`, `projectId`, `userId`).

- **Plugin frontend** — stateless; same JS for everyone.
- **Plugin backend** (if any) — multi-tenant by `installId`, usually further keyed by `projectId`.
- **Tolgee** — stores installs (one per org per plugin), enablement state per project, the install's granted scopes, and any per-install secrets exchanged during registration.

### 3.7 Auth summary

The same JWT covers both authentication needs:

| Caller | Audience | Verifier | Validates with |
| --- | --- | --- | --- |
| Plugin frontend → Tolgee REST API | Tolgee | Tolgee API gateway | Its own signing key |
| Plugin frontend → plugin's own backend | Plugin backend | Plugin backend | Tolgee's public key (JWKS) |

Permission scoping (later scope) layers two checks: the install's granted scopes (`translations:read`, `keys:write`, …) cap what the token can do, and the user's project role further constrains. Both must permit the operation.

Token rotation (later scope): tokens are short-lived. When one expires, the SDK requests a refresh via `postMessage`; Tolgee mints a new one and posts it back. The SDK retries the failed request transparently.

### 3.8 Local development model (later scope, summarized)

Loading `http://localhost:5180` into an iframe inside `https://app.tolgee.io` is **blocked by browser mixed-content rules** and there is no Tolgee-side override. The planned dev flow:

- `tolgee plugin dev` CLI auto-opens a tunnel (Cloudflare/ngrok) to a Tolgee-controlled subdomain.
- CLI registers a temporary dev-only install pointing at the tunnel.
- Both iframe loads and webhook deliveries flow through the same tunnel.
- For self-hosted Tolgee running locally over HTTP, no tunnel is needed; the CLI detects this and skips it.

Pattern is borrowed from Shopify's `app dev` command.

---

## 4. Open design questions

- **Token transport in v1.** Query string is simple but leaks the token to `Referer` and browser history. `postMessage` is leak-free but requires the SDK to be loaded before context is available. Lean toward `postMessage` once SDK exists; query string is acceptable for PoC.
- **Iframe sandboxing approach.** `<iframe sandbox>` vs per-plugin subdomain. Decide before the iframe rendering scope lands.
- **Manifest refresh policy.** Fetch only on explicit admin refresh (current plan) is the conservative default. Periodic re-fetch would let plugins push module changes silently — useful, but with auditability implications.
- **Permission scope vocabulary.** To be designed when the REST API client scope is taken.
- **Self-hosted air-gap behavior.** A plugin whose `baseUrl` points at the public internet cannot work inside an air-gapped Tolgee deployment. The future ZIP delivery model fixes this for UI-only plugins; stateful plugins inside air-gaps remain a separate question.
