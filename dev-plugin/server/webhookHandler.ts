import type { webhooks } from '@tginternal/client'

/**
 * Identifier of a webhook event Tolgee can send — keys of the `webhooks`
 * interface from the generated OpenAPI schema. Examples: `"SET_TRANSLATIONS"`,
 * `"BATCH_KEY_RESTORE"`, `"CREATE_KEY"`, `"UNKNOWN"`.
 */
export type WebhookType = keyof webhooks

/**
 * Concrete request-body type for a given webhook event.
 *
 *     type SetTranslationsPayload = WebhookPayloadFor<'SET_TRANSLATIONS'>
 */
export type WebhookPayloadFor<T extends WebhookType> =
  webhooks[T]['post']['requestBody']['content']['application/json']

/**
 * Discriminated union of every typed webhook payload Tolgee can send.
 * Use this as the parsed type for the raw POST body before dispatching
 * via {@link onWebhook}.
 */
export type AppWebhookPayload = {
  [K in WebhookType]: WebhookPayloadFor<K>
}[WebhookType]

/**
 * If `payload` matches one of the supplied activity `types`, invokes
 * `handler` with the payload narrowed to that specific type's schema.
 * Returns true when the handler ran, false otherwise — so callers can
 * chain `onWebhook(...) || onWebhook(...)` to route a single payload
 * through several typed handlers.
 *
 *     onWebhook(payload, 'SET_TRANSLATIONS', (p) => {
 *       // p: WebhookPayloadFor<'SET_TRANSLATIONS'>
 *       p.activityData?.modifiedEntities?.Translation?.forEach(...)
 *     })
 *
 *     onWebhook(payload, ['SET_TRANSLATIONS', 'BATCH_SET_TRANSLATION_STATE'], (p) => {
 *       // p: WebhookPayloadFor<'SET_TRANSLATIONS' | 'BATCH_SET_TRANSLATION_STATE'>
 *     })
 */
export function onWebhook<T extends WebhookType>(
  payload: AppWebhookPayload,
  types: T | readonly T[],
  handler: (typed: WebhookPayloadFor<T>) => void
): boolean {
  const list = Array.isArray(types) ? types : [types as T]
  const activityType = payload.activityData?.type
  if (activityType && list.includes(activityType as T)) {
    handler(payload as WebhookPayloadFor<T>)
    return true
  }
  return false
}
