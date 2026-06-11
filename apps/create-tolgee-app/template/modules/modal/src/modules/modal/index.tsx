import { useEffect, useRef, useState } from 'react'
import {
  applyTolgeeTheme,
  createTolgeeApp,
  type TolgeeApp,
  type TolgeeAppContext,
} from '@tolgee/apps-sdk/browser'

export default function Modal() {
  const [ctx, setCtx] = useState<TolgeeAppContext | null>(null)
  const appRef = useRef<TolgeeApp | null>(null)

  useEffect(() => {
    const app = createTolgeeApp()
    appRef.current = app
    app.context.then(setCtx)
    const offTheme = app.onThemeChanged(applyTolgeeTheme)
    return () => {
      offTheme()
      app.dispose()
      appRef.current = null
    }
  }, [])

  const close = (): void => appRef.current?.close()

  return (
    <main>
      <h2>{{name}} modal</h2>
      {!ctx ? (
        <p>Loading context…</p>
      ) : (
        <table className="kv">
          <tbody>
            <tr>
              <th>projectId</th>
              <td><code>{ctx.projectId}</code></td>
            </tr>
            <tr>
              <th>organizationId</th>
              <td><code>{ctx.organizationId}</code></td>
            </tr>
            <tr>
              <th>keyId</th>
              <td><code>{ctx.selection.keyId ?? '—'}</code></td>
            </tr>
          </tbody>
        </table>
      )}
      <p>
        <button type="button" onClick={close}>
          Close
        </button>
      </p>
    </main>
  )
}
