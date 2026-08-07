/** The four things Tolgee tells an app about over the lifecycle channel. */
export type TolgeeLifecycleEventType =
  | 'app.registered'
  | 'app.installed'
  | 'app.uninstalled'
  | 'app.secret.rotated'

/**
 * Which credential layer a `app.secret.rotated` delivery replaces. Only the
 * app layer exists — installs carry no credentials of their own.
 */
export type TolgeeCredentialLayer = 'app'

/**
 * App-level credentials — the app's only credentials: the token endpoint
 * derives install-scoped access tokens from them for every organization that
 * installed the app.
 */
export type DeliveredAppCredentials = {
  /** Tolgee's numeric id for the registered app. */
  id: number | null
  /** The `id` declared in the manifest. */
  appId: string | null
  name: string | null
  /** Prefixed `tgpub_`. */
  clientId: string | null
  /** Prefixed `tgpubs_`. Present on registration and on an app-level rotation. */
  clientSecret: string | null
  /** What Tolgee signs this app's deliveries with. Present on registration. */
  webhookSecret: string | null
}

export type DeliveredOrganization = {
  id: number | null
  name: string | null
  slug: string | null
}

/**
 * An installation named by a delivery. It carries no credentials — the
 * app-level ones mint tokens for it.
 */
export type DeliveredInstall = {
  installId: number | null
  /** True when the install belongs to no organization. */
  native: boolean
  organization: DeliveredOrganization | null
}

export type TolgeeLifecycleEvent = {
  type: TolgeeLifecycleEventType
  /**
   * Tolgee's id for this delivery. Every retry of it carries the same one, so
   * it is what to key on when a listener must run exactly once.
   */
  deliveryId: number | null
  /** Epoch ms Tolgee signed the delivery at. */
  timestamp: number
  /** Tolgee instance the delivery was accepted for. */
  tolgeeUrl: string
  app: DeliveredAppCredentials | null
  install: DeliveredInstall | null
  organization: DeliveredOrganization | null
  /** Which layer a rotation replaced; null for every other event. */
  rotatedLayer: TolgeeCredentialLayer | null
  /**
   * False for a first delivery, whose signature could only be checked against
   * the secret it carried itself — accepted solely because this app held no
   * credentials for the instance yet. True once the signature was checked
   * against a secret only Tolgee could know.
   */
  trusted: boolean
  /** The verified body, for fields this SDK does not model yet. */
  payload: Record<string, unknown>
}

/**
 * Maps whatever Tolgee names the event to the four types above.
 *
 * Deliberately loose: it matches on the verb, so `INSTALLED`, `APP_INSTALLED`
 * and `app.installed` all land in the same place and a naming change on the
 * server does not silently drop deliveries. `UNINSTALL` and any rotation are
 * matched before `INSTALL`, since both spellings contain it.
 */
export const normalizeEventType = (
  raw: unknown
): TolgeeLifecycleEventType | null => {
  if (typeof raw !== 'string') return null
  const token = raw.toUpperCase().replace(/[^A-Z]/g, '')
  if (token === '') return null
  if (token.includes('UNINSTALL')) return 'app.uninstalled'
  if (token.includes('REMOVE')) return 'app.uninstalled'
  if (token.includes('ROTAT')) return 'app.secret.rotated'
  if (token.includes('REGISTER')) return 'app.registered'
  if (token.includes('INSTALL')) return 'app.installed'
  return null
}
