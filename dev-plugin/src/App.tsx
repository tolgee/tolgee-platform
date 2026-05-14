import { useEffect, useMemo, useState } from 'react'
import { createApiClient } from '@tginternal/client'
import './App.css'

type KeyRow = {
  id: number
  name: string
  namespace?: string
  translations: { language: string; text?: string }[]
}

function parseContext() {
  const url = new URL(window.location.href)
  return {
    token: url.searchParams.get('token'),
    projectId: Number(url.searchParams.get('projectId')),
    apiUrl: url.searchParams.get('apiUrl'),
  }
}

function App() {
  const { token, projectId, apiUrl } = useMemo(parseContext, [])
  const [error, setError] = useState<string | null>(null)
  const [loading, setLoading] = useState(true)
  const [rows, setRows] = useState<KeyRow[]>([])
  const [projectName, setProjectName] = useState<string | null>(null)

  useEffect(() => {
    if (!token || !projectId || !apiUrl) {
      setError(
        'Missing context. Open this page through the Tolgee app sidebar.'
      )
      setLoading(false)
      return
    }

    const client = createApiClient({
      baseUrl: apiUrl,
      userToken: token,
      projectId,
      autoThrow: false,
    })

    let cancelled = false
    ;(async () => {
      try {
        const projectResp = await client.GET('/v2/projects/{projectId}', {
          params: { path: { projectId } },
        })
        if (projectResp.data && !cancelled) {
          setProjectName(projectResp.data.name)
        }

        const keysResp = await client.GET(
          '/v2/projects/{projectId}/translations',
          {
            params: {
              path: { projectId },
              query: { size: 20 },
            },
          }
        )
        if (cancelled) return
        if (keysResp.error) {
          throw new Error(
            `API error: ${JSON.stringify(keysResp.error).slice(0, 240)}`
          )
        }
        const embedded =
          (keysResp.data as unknown as {
            _embedded?: { keys?: KeyRow[] }
          })?._embedded?.keys ?? []
        setRows(embedded)
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
  }, [token, projectId, apiUrl])

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

      {loading && <p>Loading…</p>}
      {error && (
        <p style={{ color: '#b00' }} data-testid="error">
          {error}
        </p>
      )}

      {!loading && !error && (
        <>
          <p>
            This is the Tolgee dev plugin. It used a signed app token to fetch
            the first {rows.length} keys in this project via the REST API.
          </p>

          {rows.length === 0 ? (
            <p>No keys in this project yet.</p>
          ) : (
            <table className="keys">
              <thead>
                <tr>
                  <th>Key</th>
                  <th>Translations</th>
                </tr>
              </thead>
              <tbody>
                {rows.map((row) => (
                  <tr key={row.id}>
                    <td>
                      {row.namespace && (
                        <code className="ns">{row.namespace}</code>
                      )}
                      <strong>{row.name}</strong>
                    </td>
                    <td>
                      {row.translations.map((t, i) => (
                        <div key={i}>
                          <code className="lang">{t.language}</code>{' '}
                          {t.text ?? <em>—</em>}
                        </div>
                      ))}
                    </td>
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
