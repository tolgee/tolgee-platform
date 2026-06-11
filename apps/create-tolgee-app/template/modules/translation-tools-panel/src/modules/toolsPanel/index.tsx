import { useEffect, useRef, useState } from 'react'
import {
  applyTolgeeTheme,
  createTolgeeApp,
  type TolgeeApp,
  type TolgeeAppSelection,
} from '@tolgee/apps-sdk/browser'

export default function ToolsPanel() {
  const [selection, setSelection] = useState<TolgeeAppSelection>({})
  const containerRef = useRef<HTMLDivElement | null>(null)
  const appRef = useRef<TolgeeApp | null>(null)

  useEffect(() => {
    const app = createTolgeeApp()
    appRef.current = app
    app.context.then((ctx) => setSelection(ctx.selection))
    const off = app.onSelectionChanged(setSelection)
    const offTheme = app.onThemeChanged(applyTolgeeTheme)
    return () => {
      off()
      offTheme()
      app.dispose()
      appRef.current = null
    }
  }, [])

  // Tell the host how tall this panel wants to be whenever the content
  // resizes. The host sizes the iframe element to match.
  useEffect(() => {
    const el = containerRef.current
    if (!el) return
    const observer = new ResizeObserver(() => {
      appRef.current?.resize(el.scrollHeight)
    })
    observer.observe(el)
    return () => observer.disconnect()
  }, [])

  return (
    <main ref={containerRef}>
      <h2>{{name}} panel</h2>
      <table className="kv">
        <tbody>
          <tr>
            <th>keyId</th>
            <td><code>{selection.keyId ?? '—'}</code></td>
          </tr>
          <tr>
            <th>languageTag</th>
            <td><code>{selection.languageTag ?? '—'}</code></td>
          </tr>
          <tr>
            <th>translationId</th>
            <td><code>{selection.translationId ?? '—'}</code></td>
          </tr>
        </tbody>
      </table>
    </main>
  )
}
