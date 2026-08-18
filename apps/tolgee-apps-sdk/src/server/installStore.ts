import { randomBytes } from 'node:crypto'
import { mkdirSync, readFileSync, renameSync, rmSync, writeFileSync } from 'node:fs'
import { dirname, join } from 'node:path'
import { normalizeTolgeeUrl } from '../shared/url'

const STATE_DIR_NAME = '.tolgee-dev'
const STATE_FILE_NAME = 'install.json'
const STATE_FILE_VERSION = 3

/** Owner-only: the file holds the app's client secret. */
const STATE_FILE_MODE = 0o600

export type AppInstallStoreOptions = {
  /**
   * Directory the state file lives in. Defaults to `TOLGEE_APP_STATE_DIR`, and
   * to `.tolgee-dev` under the working directory when that is unset.
   */
  stateDir?: string
}

/**
 * The app-level credentials Tolgee issues once, when the app is registered —
 * the app's only long-lived credentials. The token endpoint exchanges them for
 * the short-lived install-scoped tokens everything else uses. `webhookSecret`
 * is the one Tolgee signs lifecycle deliveries with, so holding it is what
 * lets this app tell a real delivery from a forged one.
 */
export type StoredApp = {
  /** Tolgee instance the app (and therefore the credentials) belongs to. */
  tolgeeUrl: string
  /** Tolgee's numeric id for the registered app; null when nothing carried it. */
  id: number | null
  /** The `id` declared in the manifest, as Tolgee registered it. */
  appId: string | null
  /** App-level client id, prefixed `tgpub_`. */
  clientId: string | null
  /** App-level client secret, prefixed `tgpubs_`. */
  clientSecret: string | null
  webhookSecret: string | null
  /**
   * The webhook secret in force before the last rotation, still accepted during the overlap. Set
   * automatically when a delivery brings a new `webhookSecret` that replaces a different stored one,
   * so a delivery signed with either verifies until Tolgee's owner revokes the old one.
   */
  webhookSecretPrevious: string | null
  /** ISO timestamp of when Tolgee issued `clientSecret`, or null when unknown. */
  secretIssuedAt: string | null
  /** ISO timestamp of the last write. */
  updatedAt: string
}

export type AppRecord = {
  tolgeeUrl: string
  id?: number | null
  appId?: string | null
  clientId?: string | null
  clientSecret?: string | null
  webhookSecret?: string | null
  webhookSecretPrevious?: string | null
  /** Defaults to now whenever the record carries a `clientSecret`. */
  secretIssuedAt?: string | null
}

/**
 * An install as it was persisted locally. It carries no credentials — the
 * app-level ones in {@link StoredApp} mint tokens for every install — only the
 * identity an app needs to name an install without asking Tolgee first.
 */
export type StoredAppInstall = {
  /** Tolgee instance the install belongs to. */
  tolgeeUrl: string
  installId: number
  organizationId: number | null
  organizationSlug: string | null
  organizationName: string | null
  /** ISO timestamp of the last write. */
  updatedAt: string
}

export type AppInstallRecord = {
  tolgeeUrl: string
  installId: number
  organizationId?: number | null
  organizationSlug?: string | null
  organizationName?: string | null
  /**
   * Whether this install becomes the one `readStoredAppInstall()` returns.
   * Defaults to true — an app registering itself means the install it just got.
   * A lifecycle delivery passes false, so an install made by some *other*
   * organization never displaces the one this app authenticates as.
   */
  makeCurrent?: boolean
}

type StoredInstance = {
  app: StoredApp | null
  /** Install `readStoredAppInstall` resolves to; see `AppInstallRecord.makeCurrent`. */
  currentInstallId: number | null
  /** Keyed by install id — one app can be installed by many organizations. */
  installs: Record<string, StoredAppInstall>
}

type StateFile = {
  version: number
  /** Keyed by Tolgee base URL, so credentials of one instance never leak into another. */
  instances: Record<string, StoredInstance>
}

/** Absolute path of the file `saveAppInstall` writes and `readStoredAppInstall` reads. */
export const appInstallStatePath = (
  options: AppInstallStoreOptions = {}
): string => join(resolveStateDir(options), STATE_FILE_NAME)

/** App-level credentials stored for `tolgeeUrl`, or null when there are none. */
export const readStoredApp = (
  tolgeeUrl: string,
  options: AppInstallStoreOptions = {}
): StoredApp | null =>
  readInstance(readStateFile(appInstallStatePath(options)), tolgeeUrl).app

/**
 * Credentials of the install this app authenticates as on `tolgeeUrl`, or null
 * when this instance has none. See `readStoredAppInstalls` for every install.
 */
export const readStoredAppInstall = (
  tolgeeUrl: string,
  options: AppInstallStoreOptions = {}
): StoredAppInstall | null => {
  const instance = readInstance(
    readStateFile(appInstallStatePath(options)),
    tolgeeUrl
  )
  if (instance.currentInstallId === null) return null
  return instance.installs[String(instance.currentInstallId)] ?? null
}

/** Every install stored for `tolgeeUrl` — one per organization that installed the app. */
export const readStoredAppInstalls = (
  tolgeeUrl: string,
  options: AppInstallStoreOptions = {}
): StoredAppInstall[] =>
  Object.values(
    readInstance(readStateFile(appInstallStatePath(options)), tolgeeUrl).installs
  )

/** The stored install with this id, or null when it is not (or no longer) held. */
export const readStoredAppInstallById = (
  tolgeeUrl: string,
  installId: number,
  options: AppInstallStoreOptions = {}
): StoredAppInstall | null =>
  readInstance(readStateFile(appInstallStatePath(options)), tolgeeUrl).installs[
    String(installId)
  ] ?? null

/** True when anything credential-bearing is stored for `tolgeeUrl`, at either layer. */
export const hasStoredCredentials = (
  tolgeeUrl: string,
  options: AppInstallStoreOptions = {}
): boolean => {
  const instance = readInstance(
    readStateFile(appInstallStatePath(options)),
    tolgeeUrl
  )
  if (instance.app?.clientSecret != null) return true
  return instance.app?.webhookSecret != null
}

/**
 * Persists the app-level credentials, keeping any that are already stored when
 * the new record has none.
 *
 * Tolgee discloses each app-level secret once and stores only a hash, so an
 * update that reports `clientSecret: null` — anything but the registration or a
 * rotation — must never overwrite the only copy there is.
 */
export const saveApp = (
  record: AppRecord,
  options: AppInstallStoreOptions = {}
): StoredApp => {
  const path = appInstallStatePath(options)
  const key = normalizeTolgeeUrl(record.tolgeeUrl)
  const state = readStateFile(path)
  const instance = readInstance(state, key)
  const carried = instance.app

  const now = new Date().toISOString()
  // A delivery bringing a webhook secret that replaces a different stored one is a rotation: keep
  // the old one as `webhookSecretPrevious` so a delivery signed with either still verifies.
  const newWebhook = record.webhookSecret ?? null
  const carriedWebhook = carried?.webhookSecret ?? null
  const rotated =
    newWebhook !== null && carriedWebhook !== null && newWebhook !== carriedWebhook
  const stored: StoredApp = {
    tolgeeUrl: key,
    id: record.id ?? carried?.id ?? null,
    appId: record.appId ?? carried?.appId ?? null,
    clientId: record.clientId ?? carried?.clientId ?? null,
    clientSecret: record.clientSecret ?? carried?.clientSecret ?? null,
    webhookSecret: newWebhook ?? carriedWebhook,
    webhookSecretPrevious: rotated
      ? carriedWebhook
      : (record.webhookSecretPrevious ?? carried?.webhookSecretPrevious ?? null),
    secretIssuedAt:
      record.secretIssuedAt ??
      (record.clientSecret != null ? now : (carried?.secretIssuedAt ?? null)),
    updatedAt: now,
  }

  state.instances[key] = { ...instance, app: stored }
  writeStateFile(path, state)
  return stored
}

/** Persists an install, keeping stored fields the new record does not carry. */
export const saveAppInstall = (
  record: AppInstallRecord,
  options: AppInstallStoreOptions = {}
): StoredAppInstall => {
  const path = appInstallStatePath(options)
  const key = normalizeTolgeeUrl(record.tolgeeUrl)
  const state = readStateFile(path)
  const instance = readInstance(state, key)
  const carried = instance.installs[String(record.installId)]

  const now = new Date().toISOString()
  const stored: StoredAppInstall = {
    tolgeeUrl: key,
    installId: record.installId,
    organizationId: record.organizationId ?? carried?.organizationId ?? null,
    organizationSlug:
      record.organizationSlug ?? carried?.organizationSlug ?? null,
    organizationName:
      record.organizationName ?? carried?.organizationName ?? null,
    updatedAt: now,
  }

  const becomesCurrent =
    record.makeCurrent !== false || instance.currentInstallId === null
  state.instances[key] = {
    ...instance,
    currentInstallId: becomesCurrent
      ? record.installId
      : instance.currentInstallId,
    installs: { ...instance.installs, [String(record.installId)]: stored },
  }
  writeStateFile(path, state)
  return stored
}

/** Drops one install's credentials. Returns false when none were held. */
export const forgetAppInstall = (
  tolgeeUrl: string,
  installId: number,
  options: AppInstallStoreOptions = {}
): boolean => {
  const path = appInstallStatePath(options)
  const key = normalizeTolgeeUrl(tolgeeUrl)
  const state = readStateFile(path)
  const instance = readInstance(state, key)
  if (!(String(installId) in instance.installs)) return false

  const installs = { ...instance.installs }
  delete installs[String(installId)]
  const remaining = Object.values(installs)[0]
  state.instances[key] = {
    ...instance,
    currentInstallId:
      instance.currentInstallId === installId
        ? (remaining?.installId ?? null)
        : instance.currentInstallId,
    installs,
  }
  writeStateFile(path, state)
  return true
}

/**
 * Drops everything held for one Tolgee instance, both layers. Returns false
 * when nothing was held.
 */
export const forgetTolgeeInstance = (
  tolgeeUrl: string,
  options: AppInstallStoreOptions = {}
): boolean => {
  const path = appInstallStatePath(options)
  const key = normalizeTolgeeUrl(tolgeeUrl)
  const state = readStateFile(path)
  if (!(key in state.instances)) return false
  delete state.instances[key]
  writeStateFile(path, state)
  return true
}

const resolveStateDir = (options: AppInstallStoreOptions): string =>
  options.stateDir ??
  process.env.TOLGEE_APP_STATE_DIR ??
  join(process.cwd(), STATE_DIR_NAME)

const emptyInstance = (): StoredInstance => ({
  app: null,
  currentInstallId: null,
  installs: {},
})

const readInstance = (state: StateFile, tolgeeUrl: string): StoredInstance =>
  state.instances[normalizeTolgeeUrl(tolgeeUrl)] ?? emptyInstance()

const asNumber = (value: unknown): number | null =>
  typeof value === 'number' && Number.isFinite(value) ? value : null

const asString = (value: unknown): string | null =>
  typeof value === 'string' ? value : null

const asStoredApp = (tolgeeUrl: string, value: unknown): StoredApp | null => {
  if (typeof value !== 'object' || value === null) return null
  const raw = value as Record<string, unknown>
  return {
    tolgeeUrl,
    id: asNumber(raw.id),
    appId: asString(raw.appId),
    clientId: asString(raw.clientId),
    clientSecret: asString(raw.clientSecret),
    webhookSecret: asString(raw.webhookSecret),
    webhookSecretPrevious: asString(raw.webhookSecretPrevious),
    secretIssuedAt: asString(raw.secretIssuedAt),
    updatedAt: asString(raw.updatedAt) ?? '',
  }
}

const asStoredInstall = (
  tolgeeUrl: string,
  value: unknown
): StoredAppInstall | null => {
  if (typeof value !== 'object' || value === null) return null
  const raw = value as Record<string, unknown>
  if (typeof raw.installId !== 'number') return null
  return {
    tolgeeUrl,
    installId: raw.installId,
    organizationId: asNumber(raw.organizationId),
    organizationSlug: asString(raw.organizationSlug),
    organizationName: asString(raw.organizationName),
    updatedAt: asString(raw.updatedAt) ?? '',
  }
}

const asStoredInstance = (
  tolgeeUrl: string,
  value: unknown
): StoredInstance => {
  if (typeof value !== 'object' || value === null) return emptyInstance()
  const raw = value as Record<string, unknown>
  const installs: Record<string, StoredAppInstall> = {}
  if (typeof raw.installs === 'object' && raw.installs !== null) {
    for (const entry of Object.values(raw.installs)) {
      const install = asStoredInstall(tolgeeUrl, entry)
      if (install) installs[String(install.installId)] = install
    }
  }
  const current = asNumber(raw.currentInstallId)
  return {
    app: asStoredApp(tolgeeUrl, raw.app),
    currentInstallId:
      current !== null && String(current) in installs
        ? current
        : (Object.values(installs)[0]?.installId ?? null),
    installs,
  }
}

/**
 * Version 1 held exactly one install per Tolgee instance, keyed by URL, with no
 * app layer. The install identity is read forward; the per-install credentials
 * such a file carried no longer exist as a concept and are dropped.
 */
const migrateVersion1 = (installs: object): Record<string, StoredInstance> => {
  const instances: Record<string, StoredInstance> = {}
  for (const [url, value] of Object.entries(installs)) {
    const install = asStoredInstall(url, value)
    if (!install) continue
    instances[url] = {
      app: null,
      currentInstallId: install.installId,
      installs: { [String(install.installId)]: install },
    }
  }
  return instances
}

/**
 * Never throws: a missing, truncated or hand-edited file reads as "nothing
 * stored" rather than taking down the app that is only trying to boot.
 */
const readStateFile = (path: string): StateFile => {
  const empty: StateFile = { version: STATE_FILE_VERSION, instances: {} }
  let raw: string
  try {
    raw = readFileSync(path, 'utf8')
  } catch {
    return empty
  }
  let parsed: unknown
  try {
    parsed = JSON.parse(raw)
  } catch {
    return empty
  }
  if (typeof parsed !== 'object' || parsed === null) return empty

  const legacy = (parsed as { installs?: unknown }).installs
  if (typeof legacy === 'object' && legacy !== null) {
    return { version: STATE_FILE_VERSION, instances: migrateVersion1(legacy) }
  }

  const instances = (parsed as { instances?: unknown }).instances
  if (typeof instances !== 'object' || instances === null) return empty

  const valid: Record<string, StoredInstance> = {}
  for (const [key, value] of Object.entries(instances)) {
    valid[key] = asStoredInstance(key, value)
  }
  return { version: STATE_FILE_VERSION, instances: valid }
}

/**
 * Writes through a temporary file in the same directory and renames it into
 * place, so a crashed or concurrent write can never leave a half-written file
 * that a later boot would read as "no credentials".
 */
const writeStateFile = (path: string, state: StateFile): void => {
  mkdirSync(dirname(path), { recursive: true })
  const temporary = `${path}.${process.pid}.${randomBytes(4).toString('hex')}.tmp`
  try {
    writeFileSync(temporary, JSON.stringify(state, null, 2) + '\n', {
      encoding: 'utf8',
      mode: STATE_FILE_MODE,
    })
    renameSync(temporary, path)
  } catch (error) {
    rmSync(temporary, { force: true })
    throw error
  }
}
