import { useEffect, useRef, useState } from 'react'
import {
  applyTolgeeTheme,
  createTolgeeApp,
  createTolgeeAppClient,
  type TolgeeApp,
  type TolgeeAppContext,
} from '@tolgee/apps-sdk/browser'

export default function App() {
  const appRef = useRef<TolgeeApp | null>(null)
  const [context, setContext] = useState<TolgeeAppContext | null>(null)
  const [projectName, setProjectName] = useState<string | null>(null)
  const [error, setError] = useState<string | null>(null)

  useEffect(() => {
    // Set from TOLGEE_URL at build time (see vite.config.ts). Empty when the app
    // is built without one — the SDK then falls back to pinning whichever origin
    // completes the handshake first.
    const app = createTolgeeApp({
      tolgeeOrigin: import.meta.env.VITE_TOLGEE_ORIGIN || undefined,
    })
    appRef.current = app
    app.context.then(setContext)
    const offTheme = app.onThemeChanged(applyTolgeeTheme)
    return () => {
      offTheme()
      app.dispose()
      appRef.current = null
    }
  }, [])

  useEffect(() => {
    if (!context) return
    applyTolgeeTheme(context.theme)

    const tolgee = createTolgeeAppClient(context)
    tolgee
      .GET('/v2/projects/{projectId}', {
        params: { path: { projectId: context.projectId } },
      })
      .then(({ data, error: apiError }) => {
        if (apiError) setError(JSON.stringify(apiError))
        else if (data) setProjectName(data.name)
      })
      .catch((e: unknown) => setError(String(e)))
  }, [context])

  useEffect(() => {
    appRef.current?.resize(document.documentElement.scrollHeight)
  })

  if (!context) {
    return (
      <main>
        <p className="muted">Waiting for Tolgee…</p>
      </main>
    )
  }

  return (
    <main>
      <h1>{{name}}</h1>
      <p className="muted">
        Scaffolded with <code>create-tolgee-app</code>. Edit{' '}
        <code>src/App.tsx</code> to make it yours.
      </p>

      <table className="kv">
        <tbody>
          <tr>
            <th>Project</th>
            <td>{projectName ?? `#${context.projectId}`}</td>
          </tr>
          <tr>
            <th>Organization</th>
            <td>{context.organizationId ?? '—'}</td>
          </tr>
          <tr>
            <th>API</th>
            <td>
              <code>{context.apiUrl}</code>
            </td>
          </tr>
          <tr>
            <th>Theme</th>
            <td>{context.theme.mode}</td>
          </tr>
        </tbody>
      </table>

      {error && <p className="error">Tolgee API call failed: {error}</p>}
    </main>
  )
}
