import { useEffect, useState } from 'react'
import {
  createTolgeeAppClient,
  type TolgeeAppContext,
} from '@tolgee/apps-sdk/browser'

/** How many keys the page shows. Also the page size asked of Tolgee. */
export const KEY_LIMIT = 10

export type ShowcaseKey = {
  id: number
  name: string
  namespace: string | null
  /** Base-language text, or null when the key is untranslated there. */
  baseTranslation: string | null
}

export type ProjectKeys = {
  keys: ShowcaseKey[]
  /** Null when the response carried no base language (nothing to display). */
  baseLanguageTag: string | null
  /** Total keys in the project, so the page can say "showing 3 of 3". */
  total: number
}

export type ProjectKeysState =
  | { status: 'loading' }
  | { status: 'forbidden' }
  | { status: 'error'; message: string }
  | ({ status: 'ready' } & ProjectKeys)

/**
 * Loads the first {@link KEY_LIMIT} keys of the app's project with their
 * base-language translations.
 *
 * `/v2/projects/{projectId}/translations` is used rather than `…/keys`
 * because it returns the translations and the project's language list
 * (including which one is the base) in the same response.
 */
export const useProjectKeys = (
  context: TolgeeAppContext | null
): ProjectKeysState => {
  const [state, setState] = useState<ProjectKeysState>({ status: 'loading' })

  useEffect(() => {
    if (!context) return
    const abort = new AbortController()

    const load = async (): Promise<ProjectKeysState> => {
      const client = createTolgeeAppClient(context)
      const { data, response } = await client.GET(
        '/v2/projects/{projectId}/translations',
        {
          params: {
            path: { projectId: context.projectId },
            query: { size: KEY_LIMIT, sort: ['keyId,asc'] },
          },
          signal: abort.signal,
        }
      )

      if (response.status === 403) return { status: 'forbidden' }
      if (!data) {
        return {
          status: 'error',
          message: `Tolgee returned ${response.status} ${response.statusText}.`,
        }
      }

      const baseLanguage = data.selectedLanguages.find((language) => language.base)
      const keys = (data._embedded?.keys ?? []).map((key) => ({
        id: key.keyId,
        name: key.keyName,
        namespace: key.keyNamespace ?? null,
        baseTranslation: baseLanguage
          ? (key.translations[baseLanguage.tag]?.text ?? null)
          : null,
      }))

      return {
        status: 'ready',
        keys,
        baseLanguageTag: baseLanguage?.tag ?? null,
        total: data.page?.totalElements ?? keys.length,
      }
    }

    setState({ status: 'loading' })
    load()
      .then((next) => {
        if (!abort.signal.aborted) setState(next)
      })
      .catch((error: unknown) => {
        if (abort.signal.aborted) return
        setState({
          status: 'error',
          message: error instanceof Error ? error.message : String(error),
        })
      })

    return () => abort.abort()
  }, [context])

  return state
}
