import { readFileSync } from 'node:fs'
import { INSTALL_FILE, TOLGEE_URL } from './config'

type InstallRecord = {
  tolgeeUrl?: string
  clientSecret?: string
}

let cachedSecret: string | null = null
let cachedTolgeeUrl: string | null = null

const readInstallSecret = (): { clientSecret: string; tolgeeUrl: string } | null => {
  if (cachedSecret && cachedTolgeeUrl) {
    return { clientSecret: cachedSecret, tolgeeUrl: cachedTolgeeUrl }
  }
  try {
    const raw = readFileSync(INSTALL_FILE, 'utf8')
    const parsed = JSON.parse(raw) as InstallRecord
    if (!parsed.clientSecret) return null
    cachedSecret = parsed.clientSecret
    cachedTolgeeUrl = parsed.tolgeeUrl ?? TOLGEE_URL
    return { clientSecret: cachedSecret, tolgeeUrl: cachedTolgeeUrl }
  } catch {
    return null
  }
}

const authHeaders = (): Record<string, string> => {
  const cred = readInstallSecret()
  if (!cred) throw new Error('install record missing — run `npm run register`')
  return { 'X-API-Key': cred.clientSecret }
}

const tolgeeBase = (): string => readInstallSecret()?.tolgeeUrl ?? TOLGEE_URL

export const tolgeeReady = (): boolean => readInstallSecret() !== null

type Language = { id: number; tag: string; base: boolean }

type ProjectLanguages = {
  baseLang: string
  byId: Map<number, string>
}

const projectLanguagesCache = new Map<number, Promise<ProjectLanguages>>()

/**
 * Cached for the lifetime of the process. Restart `npm run dev` after
 * adding or renaming languages in Tolgee.
 */
export const getProjectLanguages = async (
  projectId: number
): Promise<ProjectLanguages> => {
  const cached = projectLanguagesCache.get(projectId)
  if (cached) return cached
  const pending = (async (): Promise<ProjectLanguages> => {
    const res = await fetch(
      `${tolgeeBase()}/v2/projects/${projectId}/languages?size=100`,
      { headers: authHeaders() }
    )
    if (!res.ok) {
      throw new Error(
        `languages lookup failed: ${res.status} ${await res.text()}`
      )
    }
    const json = (await res.json()) as {
      _embedded?: { languages?: Language[] }
    }
    const languages = json._embedded?.languages ?? []
    const base = languages.find((l) => l.base)
    if (!base) throw new Error('No base language reported for project')
    return {
      baseLang: base.tag,
      byId: new Map(languages.map((l) => [l.id, l.tag] as const)),
    }
  })()
  projectLanguagesCache.set(projectId, pending)
  return pending.catch((err) => {
    projectLanguagesCache.delete(projectId)
    throw err
  })
}

type KeyTranslations = {
  baseText: string | null
  translationsByTag: Record<string, string | null>
}

export const fetchKeyTranslations = async (
  projectId: number,
  keyId: number,
  baseLang: string,
  languageTags: string[]
): Promise<KeyTranslations> => {
  const langs = Array.from(new Set([baseLang, ...languageTags]))
  const qs = new URLSearchParams({ size: '1' })
  qs.set('filterKeyId', String(keyId))
  for (const tag of langs) qs.append('languages', tag)
  const res = await fetch(
    `${tolgeeBase()}/v2/projects/${projectId}/translations?${qs.toString()}`,
    { headers: authHeaders() }
  )
  if (!res.ok) {
    throw new Error(
      `translation lookup failed: ${res.status} ${await res.text()}`
    )
  }
  type Row = {
    translations: Record<string, { text: string | null } | undefined>
  }
  const json = (await res.json()) as { _embedded?: { keys?: Row[] } }
  const row = json._embedded?.keys?.[0]
  const baseText = row?.translations?.[baseLang]?.text ?? null
  const translationsByTag: Record<string, string | null> = {}
  for (const tag of languageTags) {
    translationsByTag[tag] = row?.translations?.[tag]?.text ?? null
  }
  return { baseText, translationsByTag }
}
