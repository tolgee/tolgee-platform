# Tolgee Apps — Architecture Notes

This document describes the architecture of the **Tolgee Apps** plugin system. It is intended as a reference for anyone working on the apps subsystem (manifest handling, iframe rendering, SDK, signed tokens, webhooks) or building a plugin against it.

Status: PoC design. Subject to revision as scopes are built. The PoC covers four extension points — **project dashboard page**, **translation tools panel**, **key-edit dialog tab**, and **action decorators** (key + translation row icons) — plus signed-token REST API access and webhooks. Inline annotations, dev CLI and ZIP delivery are planned for later scopes.

---

## 1. Overview

### 1.1 What this is

Tolgee Apps is a plugin system that lets third-party developers extend Tolgee with custom UI, backend behavior, and event reactions. The model borrows the manifest + module pattern from **Crowdin Apps** and the cryptographic auth model from **Atlassian Connect**.

For the PoC, plugins can contribute two kinds of UI:

- **Project dashboard pages** — full-area pages mounted in the project's left navigation. Each entry gets its own route and a sandboxed iframe.
- **Translation tools panels** — collapsible panels inside the existing right-hand tools panel of the translations view. Each entry shows alongside Comments / History / Machine Translation / Translation Memory and receives the currently selected key/language/translation as context.

In both cases the project/user/org identity is delivered to the iframe through a signed JWT and the plugin communicates with the Tolgee parent page via `postMessage`.

### 1.2 Core model in one paragraph

A plugin is a **web service hosted by its author**, described by a JSON **manifest**. An organization admin registers the plugin against their organization by giving Tolgee the manifest URL. Project admins then enable the registered plugin per-project. When a user navigates to an enabled plugin's page, Tolgee renders an iframe pointing at the plugin's `entry` URL and passes a short-lived **JWT signed by Tolgee** carrying the install / org / project / user identity. The iframe communicates with the Tolgee parent page via `postMessage`. The same JWT is also the credential the plugin uses to call Tolgee's REST API and (when present) the plugin's own backend.

### 1.3 What is and is not in the PoC

| Capability | PoC | Later scope |
| --- | --- | --- |
| Register plugin against organization | ✓ | |
| Enable plugin per project | ✓ | |
| `project-dashboard-page` module → sidebar entry + iframe | ✓ | |
| `translation-tools-panel` module → tools-panel tab + iframe | ✓ | |
| `key-edit-tab` module → key-edit dialog tab + iframe | ✓ | |
| `key-action` / `translation-action` icons on row + cell | ✓ | |
| `modal` module + 5 trigger surfaces (bulk, toolbar, sidebar, key-edit footer, keyboard shortcut) | ✓ | |
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

### 2.4a User opens the translations view (translation-tools-panel)

For each enabled plugin that declares `translation-tools-panel` modules, every entry contributes one collapsible panel inside the existing right-hand tools panel — alongside Comments, History, MT, TM, etc. The panel header uses the manifest `title` and `icon`.

1. The translations view fetches the project's enabled apps via `/v2/projects/{projectId}/apps` and merges `translation-tools-panel` modules into the panel list.
2. When the user expands the app panel, Tolgee mounts a sandboxed iframe whose `src` is `${baseUrl}${entry}`.
3. Token minting is identical to dashboard pages — one token per install, signed by Tolgee.
4. The current selection (keyId, languageId, languageTag, translationId) is delivered to the iframe in the `tolgee-app:init` payload (see §3.2 and §3.4).
5. When the user moves between keys or translation cells, Tolgee posts `tolgee-app:selection-changed` to the iframe so it can re-render against the new context.
6. When the iframe knows its content height it posts `tolgee-app:resize` and Tolgee sizes the iframe element accordingly.

### 2.4b User clicks an action decorator

`key-action` and `translation-action` modules surface as small icon buttons next to the existing native action icons. The icon is a **navigation shortcut** — the plugin's real UI lives in a side-panel module or a key-edit dialog tab the plugin also declares.

1. On rendering the translations view, the webapp fetches enabled apps via `/v2/projects/{projectId}/apps` and reads their `decoratorsUrl`. For each app with at least one **dynamic** action, the webapp `POST`s to `decoratorsUrl` with the visible window of `keyIds` + `languageTags`. The plugin replies with a list of `{keyId, languageTag?, actionKey, url?}` items. Static (non-dynamic) actions show on every row without any plugin call.
2. The icon row is composed from: static manifest actions ∪ dynamic plugin replies, in `(install registration order, manifest declaration order)`.
3. When the user clicks:
   - `link` action: `window.open(url, '_blank')`. The URL is either the dynamic-returned `url` or the manifest `urlTemplate` with `{keyName}` / `{keyId}` / etc. substituted.
   - `panel` action (translation-row): the webapp focuses the cell, opens the side panel, expands the referenced `translation-tools-panel` module, and scrolls it into view.
   - `tab` action (key-row): the webapp opens the key-edit dialog with the referenced `key-edit-tab` selected. Opening is allowed even without `keys.edit`: in that case the dialog renders only plugin tabs and hides the Save action.

The decorators endpoint is called **directly by the webapp**, not proxied through Tolgee's backend. The webapp authenticates with the user-context JWT it would have minted for the iframe (`POST /v2/projects/{id}/apps/{installId}/token`). The plugin must set CORS to allow the webapp origin.

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

The `icon` field on every module accepts either an emoji (e.g. `"⚙️"`) or a Tolgee native icon name from `@untitled-ui/icons-react` (e.g. `"Settings01"`) or `tg.component/CustomIcons` (e.g. `"Stars"`). Auto-detection: if the string matches a registered icon component, it renders as the native React icon; otherwise it renders as literal text (the emoji path). Unknown names fall back to literal text — choose names from the `@untitled-ui/icons-react` exports to get the native look.

```json
{
  "id": "tolgee-dev-plugin",
  "name": "Tolgee Dev Plugin",
  "version": "0.1.0",
  "baseUrl": "https://my-plugin.example.com",
  "decoratorsUrl": "https://my-plugin.example.com/decorators",
  "modules": {
    "project-dashboard-page": [
      {
        "key": "hello",
        "title": "Hello World",
        "icon": "👋",
        "entry": "/"
      }
    ],
    "translation-tools-panel": [
      {
        "key": "activity",
        "title": "Activity",
        "icon": "📈",
        "entry": "/tools-panel"
      }
    ],
    "key-edit-tab": [
      {
        "key": "audit",
        "title": "Audit",
        "icon": "🛡️",
        "entry": "/key-edit-tab/audit"
      }
    ],
    "key-action": [
      {
        "key": "view-source",
        "type": "link",
        "icon": "🔗",
        "tooltip": "View source",
        "urlTemplate": "https://example.com/source/{keyName}"
      },
      {
        "key": "open-audit",
        "type": "tab",
        "icon": "🛡️",
        "tooltip": "Audit info",
        "dynamic": true,
        "tabKey": "audit"
      }
    ],
    "translation-action": [
      {
        "key": "show-activity",
        "type": "panel",
        "icon": "📈",
        "tooltip": "Show activity",
        "dynamic": true,
        "panelKey": "activity"
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
| `modules.project-dashboard-page[].icon` | string | Sidebar entry icon (emoji or native icon name — see §3.1). |
| `modules.project-dashboard-page[].entry` | string | Path relative to `baseUrl`; the iframe will load this URL. |
| `modules.translation-tools-panel[]` | array | Panels this plugin contributes to the translations view's right-hand tools panel. |
| `modules.translation-tools-panel[].key` | string | Unique within the plugin; used as part of the panel id. |
| `modules.translation-tools-panel[].title` | string | Panel header title. |
| `modules.translation-tools-panel[].icon` | string | Panel header icon (emoji or native icon name — see §3.1). |
| `modules.translation-tools-panel[].entry` | string | Path relative to `baseUrl`; the iframe will load this URL when the panel is expanded. |
| `decoratorsUrl` | string | Absolute URL the webapp `POST`s to for dynamic action decorators. Plugin must serve it with CORS headers permitting the Tolgee origin. |
| `modules.key-edit-tab[]` | array | Tabs this plugin contributes to the key-edit dialog. |
| `modules.key-edit-tab[].key` | string | Unique within the plugin; referenced by `key-action[].tabKey`. |
| `modules.key-edit-tab[].title` | string | Tab label. |
| `modules.key-edit-tab[].icon` | string | Tab icon. |
| `modules.key-edit-tab[].entry` | string | Path relative to `baseUrl`; the iframe will load this URL. |
| `modules.key-action[]` | array | Icon-button actions added to the key row. |
| `modules.key-action[].key` | string | Unique within the plugin; matches `actionKey` in dynamic responses. |
| `modules.key-action[].type` | string | `link` or `tab`. |
| `modules.key-action[].icon` / `.tooltip` | string | Rendered on the button. |
| `modules.key-action[].dynamic` | boolean | When `true`, icon only shows for keys the plugin returns in its decorators response. |
| `modules.key-action[].urlTemplate` | string | Required for static `link`. Interpolated tokens: `{keyName}`, `{keyNamespace}`, `{keyId}`, `{projectId}`. |
| `modules.key-action[].tabKey` | string | For `type: tab`, references a declared `key-edit-tab[].key`. |
| `modules.key-action[].visibility` | enum | `always` or `on-hover`. Default `on-hover`. May be overridden per-item via the dynamic decorators response. |
| `modules.translation-action[]` | array | Icon-button actions added to translation cells. |
| `modules.translation-action[].type` | string | `link` or `panel`. |
| `modules.translation-action[].panelKey` | string | For `type: panel`, references a declared `translation-tools-panel[].key`. |
| `modules.translation-action[].urlTemplate` | string | For static `link`. Adds `{languageTag}`, `{languageId}`, `{translationId}` to the supported tokens. |
| `modules.translation-action[].visibility` | enum | `always` or `on-hover`. Default `on-hover`. May be overridden per-item via the dynamic decorators response. |
| `modules.modal[]` | array | Iframe modal definitions. Triggered by any `*-action` of type `modal` (and shortcuts). |
| `modules.modal[].key` | string | Unique within the plugin; referenced by `modalKey`. |
| `modules.modal[].title` | string | Dialog title shown in Tolgee's modal chrome. |
| `modules.modal[].entry` | string | Path relative to `baseUrl` loaded into the iframe. |
| `modules.modal[].width` / `.height` | int | Default modal size in px. Clamped to 90vw / 90vh at runtime. |
| `modules.bulk-action[]` | array | Items appended to the bulk-actions bar (visible when ≥1 keys selected). Each item has `key`, `title`, `icon?`, `type: link \| modal`, and either `urlTemplate` or `modalKey`. The modal iframe additionally receives `selectedKeyIds`. |
| `modules.translations-toolbar-action[]` | array | Items appended to the translations view toolbar (next to +KEY). Same shape as bulk-action. |
| `modules.project-menu-action[]` | array | Sidebar entries that don't navigate to a route — clicking opens a modal (or external link). Same shape. |
| `modules.shortcut[]` | array | Global keyboard shortcut registry. Each entry has `key`, `combination` (mousetrap-style, e.g. `Mod+Shift+E`), `type: link \| modal`, and either `urlTemplate` or `modalKey`. `Mod` resolves to ⌘ on macOS and Ctrl elsewhere. |

Later scopes will extend `modules` with additional types (`editor-right-panel`, `translation-decorator`, `custom-mt`, `modal`, …) and add top-level fields (`backendBaseUrl`, optionally `assets` when ZIP delivery lands).

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

#### 3.4.1 Handshake

1. On mount the iframe posts `{ type: 'tolgee-app:ready' }` to `window.parent`.
2. When Tolgee receives `ready` and has a token in hand, it replies with `tolgee-app:init` (payload below).

The init payload is the same envelope for every module type; modules just ignore fields they don't care about.

| Field | Type | Notes |
| --- | --- | --- |
| `type` | `'tolgee-app:init'` | |
| `token` | string | Signed JWT, see §3.2. |
| `apiUrl` | string | Tolgee API base URL the iframe should target. |
| `organizationId` | number | Always set. |
| `projectId` | number | Always set. |
| `keyId` | number \| null | `translation-tools-panel` only: the currently selected key, if any. |
| `languageId` | number \| null | `translation-tools-panel` only: the currently focused language, if any. |
| `languageTag` | string \| null | `translation-tools-panel` only: BCP 47 tag of the focused language. |
| `translationId` | number \| null | `translation-tools-panel` only: the focused translation cell, if any. |

#### 3.4.2 Selection updates (translation-tools-panel)

When the user moves between keys or translation cells while the tools-panel iframe is mounted, Tolgee posts:

```jsonc
{
  "type": "tolgee-app:selection-changed",
  "keyId": 42,
  "languageId": 7,
  "languageTag": "en",
  "translationId": 1234
}
```

The selection identifiers are not minted into the JWT — they would change too often. The token only carries the install / org / project / user / scopes claims. Per-key authorization is checked at REST-API call time against the user's project role.

#### 3.4.3 Iframe sizing

For modules that live inside a fixed-size container (currently `translation-tools-panel`), the iframe can request a height:

```jsonc
{ "type": "tolgee-app:resize", "height": 240 }
```

Tolgee applies the value to the iframe element (clamped to a reasonable range). For full-area modules (`project-dashboard-page`) the iframe stretches and the host ignores resize messages.

### 3.5 Decorator dynamic protocol

For modules declared `dynamic: true`, the webapp queries the plugin for per-row data as the visible window of keys/translations changes.

**Request** (webapp → plugin, frontend-direct):

```
POST {decoratorsUrl}
Authorization: Bearer <user-context-jwt>
Content-Type: application/json
{
  "projectId": 42,
  "keyIds": [1, 2, 3],
  "languageTags": ["en", "de"]
}
```

The JWT is the same install-context token minted via `POST /v2/projects/{id}/apps/{installId}/token`. Plugin verifies it offline against Tolgee's published public key (PoC dev-plugins skip verification).

**Response**:

```jsonc
{
  "items": [
    { "keyId": 1, "actionKey": "open-audit" },
    { "keyId": 2, "languageTag": "de", "actionKey": "show-activity", "count": 3, "visibility": "always" },
    { "keyId": 3, "actionKey": "view-source", "url": "https://example.com/custom-3" }
  ]
}
```

Rules:
- `actionKey` must match a `key-action` or `translation-action` `key`. Items the webapp can't resolve are dropped.
- For `link` actions, a returned `url` overrides the manifest `urlTemplate`. Missing `url` falls back to the template.
- For `panel`/`tab` actions, presence in the response is enough — no extra fields needed.
- Items without `languageTag` apply to the key row regardless of language. Items with `languageTag` apply only to that specific translation cell.
- **`count`** (integer, optional): when `> 0`, renders a small number badge over the icon (styled like the native unresolved-comments badge). `count === 0` hides the badge but keeps the icon; absent/null is treated the same. Valid for any action type and for both static and dynamic actions.
- **`visibility`** (`always` \| `on-hover`, optional): overrides the manifest's `visibility` for this specific (keyId, [languageTag]) instance. Precedence: dynamic-response `visibility` > manifest `visibility` > default `on-hover`. Lets a plugin keep an icon hover-only by default but elevate it to always-visible for items that need attention.

**CORS**: the plugin's `decoratorsUrl` must include `Access-Control-Allow-Origin` matching the Tolgee webapp's origin (or `*` for dev), and must allow the `Authorization` header.

**Caching**: the webapp caches responses per `(installId, sorted keyIds, sorted languageTags)` for ~30 seconds. The plugin can rely on webhooks (`SET_TRANSLATIONS`, `CREATE_KEY`, etc. — see below) to know when its data changes; a future iteration will let plugins push cache invalidations to Tolgee.

### 3.6 Webhook event names

A manifest's `webhooks.events` array may contain **any value matching a Tolgee `ActivityType` enum name**, e.g. `SET_TRANSLATIONS`, `CREATE_KEY`, `BATCH_KEY_RESTORE`, `GLOSSARY_TERM_CREATE`, `TASK_FINISH`, …. The full list of typed payload shapes is generated into `@tginternal/client` as the `webhooks` interface (one entry per activity type); the dev-plugin's `onWebhook(payload, type, handler)` utility narrows by `payload.activityData?.type` to give a fully-typed payload inside the handler.

Manifest validation rejects any event name that doesn't match an `ActivityType`. Unknown names return `app_manifest_invalid`.

For batch events (e.g. `BATCH_*` activities) the webhook payload doesn't inline every modified entity — it only carries the `revisionId`. To retrieve the actual modifications, pass that id to the [Get modified entities by revision](/api/get-modified-entities-by-revision) endpoint. The response is paginated and supports filtering by entity class (e.g. `Translation`, `Key`). Summary information about the revision itself is available via [Get single revision](/api/get-single-revision).

**Future** (`{branchId}`, `{branchName}`) — added when Tolgee gains branch support.

#### 3.4.4 SDK surface (later scope)

The SDK will wrap the raw API to provide an async request/response style:

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

### 3.7 Modal triggers

A `modal` module defines the iframe content; the trigger module determines **where** in the Tolgee UI it can be opened from. Each trigger entry has `type: link | modal`; modal types reference a declared modal by `modalKey`. Five trigger anchors are wired today:

| Trigger module | UI anchor | Extra `tolgee-app:init` payload |
| --- | --- | --- |
| `bulk-action` | Bulk-actions bar (visible when ≥1 keys selected). | `selectedKeyIds: number[]` |
| `translations-toolbar-action` | Top of the translations view, next to the +KEY button. | (project context only) |
| `project-menu-action` | Project sidebar — sits alongside `project-dashboard-page` entries but opens a modal instead of routing. | (project context only) |
| `shortcut` | Global keyboard shortcut bound in the translations view. | The currently-focused selection if any, else project context only. |

#### Modal lifecycle

- The webapp owns the dialog chrome (title bar + close ×). Backdrop click and ESC close it.
- The plugin can post `{ type: 'tolgee-app:close' }` from inside its iframe to close itself programmatically.
- A new `open()` call replaces the currently-open modal (single-slot policy).

#### Shortcut combination grammar

`combination` is a mousetrap-style string of `+`-separated tokens, last one being the key:

| Token | Meaning |
| --- | --- |
| `Mod` | ⌘ on macOS, Ctrl elsewhere |
| `Ctrl` / `Meta` / `Shift` / `Alt` | explicit modifiers |
| (any single character) | the trigger key — matched case-insensitively against `event.key` |

Examples: `Mod+Shift+E`, `Ctrl+K`, `Shift+/`. Last-registered shortcut wins on collisions; no conflict warning.

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
