import type {
  AppWebhookPayload,
  WebhookPayloadFor,
  WebhookType,
} from '../shared/webhookTypes'

/**
 * If `payload` matches one of the supplied activity `types`, invokes
 * `handler` with the payload narrowed to that specific type's schema.
 * Returns true when the handler ran.
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
