# apps/

Code related to the Tolgee Apps plugin system lives here.

## Layout

- `tolgee-apps-sdk/` — the SDK package that plugin authors install
  (`@tolgee/apps-sdk`). Currently a skeleton; will hold the typed
  postMessage handshake, manifest schema, webhook signature verifier,
  and JWT context helpers. To be published from this directory once
  the contract stabilises.
- `example-apps/` — runnable example plugins built against the SDK.
  - `dev-plugin/` — the in-tree reference plugin used during PoC
    development. Demonstrates every module type and serves as the
    template `create-tolgee-app` will copy from.
