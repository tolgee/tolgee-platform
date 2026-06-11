import { readFileSync } from 'node:fs'
import { INSTALL_FILE, TOLGEE_URL } from './config'

type InstallRecord = {
  tolgeeUrl?: string
  clientSecret?: string
  organizationId?: number
}

type Install = {
  clientSecret: string
  tolgeeUrl: string
  organizationId: number
}

let cached: Install | null = null

const readInstall = (): Install | null => {
  if (cached) return cached
  try {
    const parsed = JSON.parse(readFileSync(INSTALL_FILE, 'utf8')) as InstallRecord
    if (!parsed.clientSecret || typeof parsed.organizationId !== 'number') return null
    cached = {
      clientSecret: parsed.clientSecret,
      tolgeeUrl: parsed.tolgeeUrl ?? TOLGEE_URL,
      organizationId: parsed.organizationId,
    }
    return cached
  } catch {
    return null
  }
}

const install = (): Install => {
  const rec = readInstall()
  if (!rec) throw new Error('install record missing — run `npm run register`')
  return rec
}

const authHeaders = (): Record<string, string> => ({
  'X-API-Key': install().clientSecret,
})

const tolgeeBase = (): string => install().tolgeeUrl

export const tolgeeReady = (): boolean => readInstall() !== null

/** The organization this install belongs to — owns the glossaries it may write. */
export const getOrganizationId = (): number => install().organizationId

type Language = { id: number; tag: string; base: boolean }

type ProjectLanguages = { baseLang: string; byId: Map<number, string> }

const projectLanguagesCache = new Map<number, Promise<ProjectLanguages>>()

/** Cached for the process lifetime. Restart `npm run dev` after changing languages. */
export const getProjectLanguages = async (
  projectId: number
): Promise<ProjectLanguages> => {
  const cachedLangs = projectLanguagesCache.get(projectId)
  if (cachedLangs) return cachedLangs
  const pending = (async (): Promise<ProjectLanguages> => {
    const res = await fetch(
      `${tolgeeBase()}/v2/projects/${projectId}/languages?size=100`,
      { headers: authHeaders() }
    )
    if (!res.ok) {
      throw new Error(`languages lookup failed: ${res.status} ${await res.text()}`)
    }
    const json = (await res.json()) as { _embedded?: { languages?: Language[] } }
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

type KeyContext = {
  keyName: string
  baseText: string | null
  text: string | null
}

export const fetchKeyContext = async (
  projectId: number,
  keyId: number,
  baseLang: string,
  languageTag: string
): Promise<KeyContext> => {
  const langs = Array.from(new Set([baseLang, languageTag]))
  const qs = new URLSearchParams({ size: '1' })
  qs.set('filterKeyId', String(keyId))
  for (const tag of langs) qs.append('languages', tag)
  const res = await fetch(
    `${tolgeeBase()}/v2/projects/${projectId}/translations?${qs.toString()}`,
    { headers: authHeaders() }
  )
  if (!res.ok) {
    throw new Error(`translation lookup failed: ${res.status} ${await res.text()}`)
  }
  type Row = {
    keyName: string
    translations: Record<string, { text: string | null } | undefined>
  }
  const json = (await res.json()) as { _embedded?: { keys?: Row[] } }
  const row = json._embedded?.keys?.[0]
  return {
    keyName: row?.keyName ?? `key#${keyId}`,
    baseText: row?.translations?.[baseLang]?.text ?? null,
    text: row?.translations?.[languageTag]?.text ?? null,
  }
}

const jsonHeaders = (): Record<string, string> => ({
  ...authHeaders(),
  'Content-Type': 'application/json',
})

/**
 * Creates a glossary term with its base-language text. Requires the install to hold the
 * `glossary.edit` org scope (see the platform's app org-level authorization).
 */
export const createGlossaryTerm = async (
  glossaryId: number,
  text: string,
  description: string
): Promise<number> => {
  const orgId = getOrganizationId()
  const res = await fetch(
    `${tolgeeBase()}/v2/organizations/${orgId}/glossaries/${glossaryId}/terms`,
    {
      method: 'POST',
      headers: jsonHeaders(),
      body: JSON.stringify({ text, description }),
    }
  )
  if (!res.ok) {
    throw new Error(`glossary term create failed: ${res.status} ${await res.text()}`)
  }
  const json = (await res.json()) as { term?: { id?: number } }
  const termId = json.term?.id
  if (typeof termId !== 'number') throw new Error('glossary term create returned no id')
  return termId
}

/** Adds (or updates) the translation of a term in a given language. */
export const setGlossaryTranslation = async (
  glossaryId: number,
  termId: number,
  languageTag: string,
  text: string
): Promise<void> => {
  const orgId = getOrganizationId()
  const res = await fetch(
    `${tolgeeBase()}/v2/organizations/${orgId}/glossaries/${glossaryId}/terms/${termId}/translations`,
    {
      method: 'POST',
      headers: jsonHeaders(),
      body: JSON.stringify({ languageTag, text }),
    }
  )
  if (!res.ok) {
    throw new Error(`glossary translation set failed: ${res.status} ${await res.text()}`)
  }
}
