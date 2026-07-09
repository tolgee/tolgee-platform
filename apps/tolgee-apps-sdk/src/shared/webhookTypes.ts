import type { webhooks } from '@tginternal/client'

/**
 * Identifier of a webhook event Tolgee can send — keys of the `webhooks`
 * interface from the generated OpenAPI schema. Examples: `"SET_TRANSLATIONS"`,
 * `"BATCH_KEY_RESTORE"`, `"CREATE_KEY"`.
 */
export type WebhookType = keyof webhooks

/**
 * Request-body type for a given webhook event.
 *
 *     type SetTranslationsPayload = WebhookPayloadFor<'SET_TRANSLATIONS'>
 */
export type WebhookPayloadFor<T extends WebhookType> =
  webhooks[T]['post']['requestBody']['content']['application/json']

/**
 * Discriminated union of every typed webhook payload Tolgee can send.
 * Use this as the parsed type for the raw POST body before dispatching
 * via `onWebhook` from `@tolgee/apps-sdk/server`.
 */
export type AppWebhookPayload = {
  [K in WebhookType]: WebhookPayloadFor<K>
}[WebhookType]
