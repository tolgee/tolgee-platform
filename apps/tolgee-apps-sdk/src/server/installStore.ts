import { randomBytes } from 'node:crypto'
import { mkdirSync, readFileSync, renameSync, rmSync, writeFileSync } from 'node:fs'
import { dirname, join } from 'node:path'
import { normalizeTolgeeUrl } from '../shared/url'

const STATE_DIR_NAME = '.tolgee-dev'
const STATE_FILE_NAME = 'install.json'
const STATE_FILE_VERSION = 1

/** Owner-only: the file holds the app's client secret. */
const STATE_FILE_MODE = 0o600

export type AppInstallStoreOptions = {
  /**
   * Directory the state file lives in. Defaults to `TOLGEE_APP_STATE_DIR`, and
   * to `.tolgee-dev` under the working directory when that is unset.
   */
  stateDir?: string
}

/** An install as it was persisted locally, with the credentials Tolgee issued. */
export type StoredAppInstall = {
  /** Tolgee instance the install (and therefore the credentials) belongs to. */
  tolgeeUrl: string
  installId: number
  clientId: string | null
  clientSecret: string | null
  /** True when the install belongs to no organization. */
  native: boolean
  organizationSlug: string | null
  /** ISO timestamp of the last write. */
  updatedAt: string
}

export type AppInstallRecord = {
  tolgeeUrl: string
  installId: number
  clientId?: string | null
  clientSecret?: string | null
  native?: boolean
  organizationSlug?: string | null
}

type StateFile = {
  version: number
  /** Keyed by Tolgee base URL, so credentials of one instance never leak into another. */
  installs: Record<string, StoredAppInstall>
}

/** Absolute path of the file `saveAppInstall` writes and `readStoredAppInstall` reads. */
export const appInstallStatePath = (
  options: AppInstallStoreOptions = {}
): string => join(resolveStateDir(options), STATE_FILE_NAME)

/** Credentials stored for `tolgeeUrl`, or null when this instance has none. */
export const readStoredAppInstall = (
  tolgeeUrl: string,
  options: AppInstallStoreOptions = {}
): StoredAppInstall | null =>
  readStateFile(appInstallStatePath(options)).installs[
    normalizeTolgeeUrl(tolgeeUrl)
  ] ?? null

/**
 * Persists an install, keeping credentials that are already stored for the same
 * install when the new record has none.
 *
 * Tolgee returns the client secret only when it creates the install — every
 * later registration reports `clientSecret: null`, and overwriting with that
 * would destroy the only copy of a secret Tolgee will not show again.
 */
export const saveAppInstall = (
  record: AppInstallRecord,
  options: AppInstallStoreOptions = {}
): StoredAppInstall => {
  const path = appInstallStatePath(options)
  const key = normalizeTolgeeUrl(record.tolgeeUrl)
  const state = readStateFile(path)
  const previous = state.installs[key]
  const carried =
    previous && isSameInstall(previous, record) ? previous : undefined

  const stored: StoredAppInstall = {
    tolgeeUrl: key,
    installId: record.installId,
    clientId: record.clientId ?? carried?.clientId ?? null,
    clientSecret: record.clientSecret ?? carried?.clientSecret ?? null,
    native: record.native ?? carried?.native ?? false,
    organizationSlug:
      record.organizationSlug ?? carried?.organizationSlug ?? null,
    updatedAt: new Date().toISOString(),
  }

  state.installs[key] = stored
  writeStateFile(path, state)
  return stored
}

const resolveStateDir = (options: AppInstallStoreOptions): string =>
  options.stateDir ??
  process.env.TOLGEE_APP_STATE_DIR ??
  join(process.cwd(), STATE_DIR_NAME)

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
    clientId: typeof raw.clientId === 'string' ? raw.clientId : null,
    clientSecret: typeof raw.clientSecret === 'string' ? raw.clientSecret : null,
    native: raw.native === true,
    organizationSlug:
      typeof raw.organizationSlug === 'string' ? raw.organizationSlug : null,
    updatedAt: typeof raw.updatedAt === 'string' ? raw.updatedAt : '',
  }
}

/**
 * Never throws: a missing, truncated or hand-edited file reads as "nothing
 * stored" rather than taking down the app that is only trying to boot.
 */
const readStateFile = (path: string): StateFile => {
  const empty: StateFile = { version: STATE_FILE_VERSION, installs: {} }
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
  const installs = (parsed as { installs?: unknown }).installs
  if (typeof installs !== 'object' || installs === null) return empty

  const valid: Record<string, StoredAppInstall> = {}
  for (const [key, value] of Object.entries(installs)) {
    const record = asStoredInstall(key, value)
    if (record) valid[key] = record
  }
  return { version: STATE_FILE_VERSION, installs: valid }
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

const isSameInstall = (
  stored: StoredAppInstall,
  record: AppInstallRecord
): boolean =>
  stored.installId === record.installId &&
  (record.clientId == null ||
    stored.clientId == null ||
    stored.clientId === record.clientId)
