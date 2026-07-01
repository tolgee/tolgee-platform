import { useEffect, useRef, useState } from 'react'
import {
  applyTolgeeTheme,
  createTolgeeApp,
  type TolgeeApp,
  type TolgeeAppSelection,
} from '@tolgee/apps-sdk/browser'

export default function ToolsPanelEmpty() {
  const [selection, setSelection] = useState<TolgeeAppSelection>({})
  const containerRef = useRef<HTMLDivElement | null>(null)
  const appRef = useRef<TolgeeApp | null>(null)

  useEffect(() => {
    const app = createTolgeeApp()
    appRef.current = app
    app.context.then((ctx) => setSelection(ctx.selection))
    // Fires when the user changes the language selector, too.
    const off = app.onSelectionChanged(setSelection)
    const offTheme = app.onThemeChanged(applyTolgeeTheme)
    return () => {
      off()
      offTheme()
      app.dispose()
      appRef.current = null
    }
  }, [])

  useEffect(() => {
    const el = containerRef.current
    if (!el) return
    const observer = new ResizeObserver(() => {
      appRef.current?.resize(el.scrollHeight)
    })
    observer.observe(el)
    return () => observer.disconnect()
  }, [])

  const languages = selection.selectedLanguages ?? []

  return (
    <main ref={containerRef}>
      <h2>{{name}} — selected languages</h2>
      {languages.length === 0 ? (
        <p>No languages selected.</p>
      ) : (
        <ul>
          {languages.map((tag) => (
            <li key={tag}>
              <code>{tag}</code>
            </li>
          ))}
        </ul>
      )}
    </main>
  )
}
