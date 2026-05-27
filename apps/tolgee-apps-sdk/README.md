# @tolgee/apps-sdk

SDK for building Tolgee Apps plugins. **Skeleton** — public API not yet stable.

## What lives here (planned)

- **Manifest schema** — Zod-typed authoring (`defineManifest({...})`) so
  module-key typos and missing required fields fail at build time.
- **postMessage handshake** — typed helpers for the iframe side of the
  `tolgee-app:ready` / `tolgee-app:init` / `tolgee-app:resize` /
  `tolgee-app:selection-changed` / `tolgee-app:close` protocol.
- **JWT context decoder** — read `tg.app.inst`, `tg.app.proj`, `sub` out
  of the init token so a plugin can call back into Tolgee's REST API as
  the install + user.
- **Webhook signature verifier** — middleware matching the
  `Tolgee-Signature: {"timestamp": <ms>, "signature": "<hex>"}` header
  scheme. HMAC-SHA256 over `${timestamp}.${rawBody}`, constant-time
  comparison.
- **Typed webhook handler dispatcher** — narrow `payload.activityData.type`
  to give plugin authors a fully-typed payload per `ActivityType`.

Reference implementation today lives in
[`example-apps/dev-plugin`](../example-apps/dev-plugin/). Code will be
lifted from there as the contract stabilises.
