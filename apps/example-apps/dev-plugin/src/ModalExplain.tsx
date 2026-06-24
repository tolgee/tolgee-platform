import { useEffect, useRef, useState } from 'react'
import {
  applyTolgeeTheme,
  createTolgeeApp,
  type TolgeeApp,
  type TolgeeAppContext,
} from '@tolgee/apps-sdk/browser'
import './App.css'

function ModalExplain() {
  const [context, setContext] = useState<TolgeeAppContext | null>(null)
  const appRef = useRef<TolgeeApp | null>(null)

  useEffect(() => {
    const app = createTolgeeApp()
    appRef.current = app
    app.context.then(setContext)
    const offTheme = app.onThemeChanged(applyTolgeeTheme)
    return () => {
      offTheme()
      app.dispose()
      appRef.current = null
    }
  }, [])

  const close = () => appRef.current?.close()

  return (
    <main className="modal-explain">
      <h2>💡 Plugin modal — init context</h2>
      {!context ? (
        <p>Waiting for host context…</p>
      ) : (
        <table className="kv">
          <tbody>
            {(
              [
                ['projectId', context.projectId],
                ['organizationId', context.organizationId],
                ['keyId', context.selection.keyId],
                ['languageId', context.selection.languageId],
                ['languageTag', context.selection.languageTag],
                ['translationId', context.selection.translationId],
                ['selectedKeyIds', formatList(context.extra.selectedKeyIds)],
              ] as Array<[string, unknown]>
            )
              .filter(([, value]) => value !== undefined && value !== null && value !== '')
              .map(([key, value]) => (
                <tr key={key}>
                  <th>{key}</th>
                  <td>
                    <code>{String(value)}</code>
                  </td>
                </tr>
              ))}
          </tbody>
        </table>
      )}
      <div className="modal-actions">
        <button type="button" onClick={close}>
          Close
        </button>
      </div>
    </main>
  )
}

const formatList = (value: unknown): string | undefined => {
  return Array.isArray(value) ? value.join(', ') : undefined
}

export default ModalExplain
