/**
 * @tolgee/apps-sdk — public surface for Tolgee Apps plugin authors.
 *
 * Skeleton package. Real exports land as the contract stabilises:
 * - Manifest schema + builder (zod-typed authoring)
 * - postMessage handshake (`tolgee-app:ready`, `tolgee-app:init`,
 *   `tolgee-app:close`, `tolgee-app:resize`, `tolgee-app:selection-changed`)
 * - JWT context decoder for iframe modules
 * - Webhook signature verifier matching Tolgee's `Tolgee-Signature` header
 * - Typed webhook handler dispatcher
 */
export {}
