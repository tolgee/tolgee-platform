/**
 * @tolgee/apps-sdk — shared types for both iframe and backend code.
 * Environment-specific entry points: `@tolgee/apps-sdk/browser` and
 * `@tolgee/apps-sdk/server`.
 */
export type {
  AppWebhookPayload,
  WebhookPayloadFor,
  WebhookType,
} from './shared/webhookTypes'
export type {
  AppContextClaims,
  TolgeeAppContext,
  TolgeeAppSelection,
} from './shared/contextTypes'
