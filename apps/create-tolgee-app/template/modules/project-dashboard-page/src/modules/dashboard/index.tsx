import { useEffect, useState } from 'react'
import {
  applyTolgeeTheme,
  createTolgeeApp,
  createTolgeeAppClient,
  type TolgeeAppContext,
} from '@tolgee/apps-sdk/browser'

export default function Dashboard() {
  const [ctx, setCtx] = useState<TolgeeAppContext | null>(null)
  const [projectName, setProjectName] = useState<string | null>(null)
  const [error, setError] = useState<string | null>(null)

  useEffect(() => {
    const app = createTolgeeApp()
    app.context.then(setCtx)
    const offTheme = app.onThemeChanged(applyTolgeeTheme)
    return () => {
      offTheme()
      app.dispose()
    }
  }, [])

  useEffect(() => {
    if (!ctx) return
    const tolgee = createTolgeeAppClient(ctx)
    tolgee
      .GET('/v2/projects/{projectId}', {
        params: { path: { projectId: ctx.projectId } },
      })
      .then(({ data, error }) => {
        if (error) {
          setError(JSON.stringify(error))
        } else if (data) {
          setProjectName(data.name)
        }
      })
  }, [ctx])

  if (!ctx) return <main>Loading…</main>
  return (
    <main>
      <h1>{{name}}</h1>
      <p>
        Hello from project <strong>{projectName ?? ctx.projectId}</strong>.
      </p>
      {error && <pre style={{ color: 'crimson' }}>{error}</pre>}
    </main>
  )
}
