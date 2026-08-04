/**
 * Machine-to-machine demo — the "cron job" story.
 *
 * No browser, no iframe, no user: the app exchanges its client credentials for
 * an access token and reads its project's keys straight from a backend script.
 *
 *   npm run token
 *
 * Requires TOLGEE_APP_CLIENT_ID / TOLGEE_APP_CLIENT_SECRET (printed once when
 * the app registers) and TOLGEE_PROJECT_ID.
 */
import {
  fetchAppAccessToken,
  loadTolgeeAppConfig,
} from '@tolgee/apps-sdk/server'

const KEY_LIMIT = 10

type TranslationsPage = {
  _embedded?: {
    keys?: {
      keyName: string
      keyNamespace?: string
      translations: Record<string, { text?: string } | undefined>
    }[]
  }
  selectedLanguages: { tag: string; base: boolean }[]
}

// Annotated on the declaration so TypeScript treats `fail(...)` as terminating
// control flow, which is what narrows the config values below.
const fail: (message: string) => never = (message) => {
  console.error(message)
  process.exit(1)
}

const config = loadTolgeeAppConfig()
const projectId = Number(process.env.TOLGEE_PROJECT_ID)

if (!config.clientId || !config.clientSecret) {
  fail(
    'Missing app credentials. Set TOLGEE_APP_CLIENT_ID and TOLGEE_APP_CLIENT_SECRET in .env.local.\n' +
      'They are printed once, when the app registers — see the README ("Auto-connect mode").'
  )
}

if (!Number.isInteger(projectId) || projectId <= 0) {
  fail(
    'Missing project. Set TOLGEE_PROJECT_ID in .env.local to the id of a project ' +
      'this app is enabled for (it is in the project URL: /projects/<id>/…).'
  )
}

const { accessToken, expiresIn } = await fetchAppAccessToken({
  tolgeeUrl: config.tolgeeUrl,
  clientId: config.clientId,
  clientSecret: config.clientSecret,
})

console.log(`Got an app access token from ${config.tolgeeUrl} (valid ${expiresIn}s).\n`)

const url = new URL(
  `/v2/projects/${projectId}/translations`,
  config.tolgeeUrl
)
url.searchParams.set('size', String(KEY_LIMIT))
url.searchParams.set('sort', 'keyId,asc')

const response = await fetch(url, {
  headers: { Authorization: `Bearer ${accessToken}` },
})

if (!response.ok) {
  fail(
    `Reading keys of project ${projectId} failed: ${response.status} ${response.statusText}\n` +
      (await response.text()) +
      (response.status === 403
        ? '\nIs the app enabled for this project, with the keys.view scope granted?'
        : '')
  )
}

const page = (await response.json()) as TranslationsPage
const baseLanguage = page.selectedLanguages.find((language) => language.base)
const keys = page._embedded?.keys ?? []

if (keys.length === 0) {
  console.log(`Project ${projectId} has no keys yet.`)
} else {
  console.log(`First ${keys.length} keys of project ${projectId}:`)
  for (const key of keys) {
    const name = key.keyNamespace ? `${key.keyNamespace}:${key.keyName}` : key.keyName
    const text = baseLanguage
      ? (key.translations[baseLanguage.tag]?.text ?? '(not translated)')
      : '(no base language)'
    console.log(`  ${name} — ${text}`)
  }
}
