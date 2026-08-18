import {
  normalizeEventType,
  type DeliveredAppCredentials,
  type DeliveredInstall,
  type DeliveredOrganization,
  type TolgeeCredentialLayer,
  type TolgeeLifecycleEventType,
} from './events'

export type ParsedDelivery = {
  type: TolgeeLifecycleEventType
  /** Tolgee's id for this delivery. Every retry of it carries the same one. */
  deliveryId: number | null
  app: DeliveredAppCredentials | null
  install: DeliveredInstall | null
  organization: DeliveredOrganization | null
  rotatedLayer: TolgeeCredentialLayer | null
  /** Instance the delivery claims to come from, when it says so. */
  tolgeeUrl: string | null
  payload: Record<string, unknown>
}

/**
 * Reads a delivery body into the shape the SDK works with, ignoring anything it
 * does not model.
 *
 * Every lookup accepts several spellings and both a nested and a flattened
 * layout on purpose: an app in the wild must keep receiving deliveries from a
 * Tolgee that has since renamed or moved a field, and dropping a delivery that
 * carries credentials is far more expensive than carrying a few aliases.
 */
export const parseDelivery = (body: string): ParsedDelivery | null => {
  let parsed: unknown
  try {
    parsed = JSON.parse(body)
  } catch {
    return null
  }
  if (typeof parsed !== 'object' || parsed === null || Array.isArray(parsed)) {
    return null
  }
  const payload = parsed as Record<string, unknown>

  const rawEventType = payload.eventType ?? payload.event ?? payload.type
  const type = normalizeEventType(rawEventType)
  if (type === null) return null

  const appNode = readAppNode(payload)
  const installNode = readInstallNode(payload)
  // The manifest id lives at the top level, next to the event, not inside the
  // credentials — so an app is named even by a delivery that carries none.
  const appId = stringAt(payload, 'appId', 'manifestId')
  const app = appNode ? withAppId(toAppCredentials(appNode), appId) : bareApp(appId)
  const install = installNode && toInstall(installNode)
  const organization =
    toOrganization(objectAt(payload, 'organization', 'org')) ??
    install?.organization ??
    null

  return {
    type,
    deliveryId: numberAt(payload, 'deliveryId'),
    app,
    install,
    organization,
    rotatedLayer: type === 'app.secret.rotated' ? rotatedLayer(app) : null,
    tolgeeUrl:
      stringAt(
        payload,
        'tolgeeInstanceUrl',
        'tolgeeUrl',
        'instanceUrl',
        'serverUrl'
      ) ?? null,
    payload,
  }
}

const withAppId = (
  app: DeliveredAppCredentials,
  appId: string | null
): DeliveredAppCredentials =>
  app.appId !== null ? app : { ...app, appId }

const bareApp = (appId: string | null): DeliveredAppCredentials | null =>
  appId === null
    ? null
    : {
        id: null,
        appId,
        name: null,
        clientId: null,
        clientSecret: null,
        webhookSecret: null,
      }

/** A rotation always replaces an app-level secret; null when none was carried. */
const rotatedLayer = (
  app: DeliveredAppCredentials | null
): TolgeeCredentialLayer | null => {
  if (app?.clientSecret != null || app?.webhookSecret != null) return 'app'
  return null
}

const readAppNode = (
  payload: Record<string, unknown>
): Record<string, unknown> | null => {
  const nested = objectAt(payload, 'app', 'appCredentials')
  if (nested) return nested
  // Flattened: an app-level credential is the one that carries a webhook secret
  // or a `tgpub_` client id.
  if (typeof payload.webhookSecret === 'string') return payload
  if (
    typeof payload.clientId === 'string' &&
    payload.clientId.startsWith('tgpub_')
  ) {
    return payload
  }
  return null
}

const readInstallNode = (
  payload: Record<string, unknown>
): Record<string, unknown> | null => {
  const nested = objectAt(
    payload,
    'install',
    'appInstall',
    'installation',
    'installCredentials'
  )
  if (nested) return nested
  if (numberAt(payload, 'installId', 'appInstallId') !== null) return payload
  if (
    typeof payload.clientId === 'string' &&
    payload.clientId.startsWith('tgapp_')
  ) {
    return payload
  }
  return null
}

const toAppCredentials = (
  node: Record<string, unknown>
): DeliveredAppCredentials => ({
  id: numberAt(node, 'id', 'appDbId'),
  // `id` is the manifest id when it is a string, and Tolgee's row id when it is
  // a number — the two collide on one field name.
  appId: stringAt(node, 'appId', 'manifestId', 'id'),
  name: stringAt(node, 'name'),
  clientId: stringAt(node, 'clientId'),
  clientSecret: stringAt(node, 'clientSecret', 'secret'),
  webhookSecret: stringAt(node, 'webhookSecret', 'signingSecret'),
})

const toInstall = (node: Record<string, unknown>): DeliveredInstall => ({
  installId: numberAt(node, 'installId', 'appInstallId', 'id'),
  organization: toOrganization(objectAt(node, 'organization', 'org')),
})

const toOrganization = (
  node: Record<string, unknown> | null
): DeliveredOrganization | null => {
  if (node === null) return null
  return {
    id: numberAt(node, 'id', 'organizationId'),
    name: stringAt(node, 'name'),
    slug: stringAt(node, 'slug'),
  }
}

const objectAt = (
  node: Record<string, unknown>,
  ...keys: string[]
): Record<string, unknown> | null => {
  for (const key of keys) {
    const value = node[key]
    if (typeof value === 'object' && value !== null && !Array.isArray(value)) {
      return value as Record<string, unknown>
    }
  }
  return null
}

const stringAt = (
  node: Record<string, unknown>,
  ...keys: string[]
): string | null => {
  for (const key of keys) {
    const value = node[key]
    if (typeof value === 'string' && value !== '') return value
  }
  return null
}

const numberAt = (
  node: Record<string, unknown>,
  ...keys: string[]
): number | null => {
  for (const key of keys) {
    const value = node[key]
    if (typeof value === 'number' && Number.isFinite(value)) return value
  }
  return null
}
