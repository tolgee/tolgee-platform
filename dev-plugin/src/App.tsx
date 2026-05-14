import { useEffect, useState } from 'react'
import { createApiClient, type components } from '@tginternal/client'
import './App.css'

type KeyRow = components['schemas']['KeyWithTranslationsModel']
type LanguageModel = components['schemas']['LanguageModel']

type AppContext = {
  token: string
  projectId: number
  apiUrl: string
}

function App() {
  const [context, setContext] = useState<AppContext | null>(null)
  const [error, setError] = useState<string | null>(null)
  const [loading, setLoading] = useState(true)
  const [keys, setKeys] = useState<KeyRow[]>([])
  const [languages, setLanguages] = useState<LanguageModel[]>([])
  const [projectName, setProjectName] = useState<string | null>(null)

  useEffect(() => {
    const handler = (event: MessageEvent) => {
      if (event.data?.type !== 'tolgee-app:init') return
      const { token, projectId, apiUrl } = event.data
      if (
        typeof token !== 'string' ||
        typeof projectId !== 'number' ||
        typeof apiUrl !== 'string'
      ) {
        setError('Received malformed init message from host.')
        setLoading(false)
        return
      }
      setContext({ token, projectId, apiUrl })
    }
    window.addEventListener('message', handler)
    window.parent.postMessage({ type: 'tolgee-app:ready' }, '*')
    return () => window.removeEventListener('message', handler)
  }, [])

  useEffect(() => {
    if (!context) return

    const client = createApiClient({
      baseUrl: context.apiUrl,
      userToken: context.token,
      projectId: context.projectId,
      autoThrow: false,
    })

    let cancelled = false
    setLoading(true)
    setError(null)
    ;(async () => {
      try {
        const projectResp = await client.GET('/v2/projects/{projectId}', {
          params: { path: { projectId: context.projectId } },
        })
        if (projectResp.error) {
          throw new Error(
            `Project lookup failed: ${JSON.stringify(projectResp.error).slice(
              0,
              240
            )}`
          )
        }
        if (cancelled) return
        if (projectResp.data) setProjectName(projectResp.data.name)

        const keysResp = await client.GET(
          '/v2/projects/{projectId}/translations',
          {
            params: { path: { projectId: context.projectId }, query: { size: 20 } },
          }
        )
        if (cancelled) return
        if (keysResp.error) {
          throw new Error(
            `Translations lookup failed: ${JSON.stringify(
              keysResp.error
            ).slice(0, 240)}`
          )
        }
        setKeys(keysResp.data?._embedded?.keys ?? [])
        setLanguages(keysResp.data?.selectedLanguages ?? [])
      } catch (e) {
        if (!cancelled) {
          setError(e instanceof Error ? e.message : String(e))
        }
      } finally {
        if (!cancelled) setLoading(false)
      }
    })()

    return () => {
      cancelled = true
    }
  }, [context])

  return (
    <main className="plugin-page">
      <h1>
        Hello World 👋
        {projectName && (
          <small style={{ marginLeft: 8, color: '#666' }}>
            from {projectName}
          </small>
        )}
      </h1>

      {!context && <p>Waiting for context from the host…</p>}
      {context && loading && <p>Loading…</p>}
      {error && (
        <p style={{ color: '#b00' }} data-testid="error">
          {error}
        </p>
      )}

      {context && !loading && !error && (
        <>
          <p>
            This is the Tolgee dev plugin. It used a signed app token to fetch
            the first {keys.length} keys in this project via the REST API.
          </p>

          {keys.length === 0 ? (
            <p>No keys in this project yet.</p>
          ) : (
            <table className="keys">
              <thead>
                <tr>
                  <th>Key</th>
                  {languages.map((lang) => (
                    <th key={lang.tag}>
                      <code className="lang">{lang.tag}</code> {lang.name}
                    </th>
                  ))}
                </tr>
              </thead>
              <tbody>
                {keys.map((row) => (
                  <tr key={row.keyId}>
                    <td>
                      {row.keyNamespace && (
                        <code className="ns">{row.keyNamespace}</code>
                      )}
                      <strong>{row.keyName}</strong>
                    </td>
                    {languages.map((lang) => {
                      const t = row.translations[lang.tag]
                      return <td key={lang.tag}>{t?.text ?? <em>—</em>}</td>
                    })}
                  </tr>
                ))}
              </tbody>
            </table>
          )}
        </>
      )}
    </main>
  )
}

export default App
