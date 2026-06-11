import { useEffect, useRef, useState } from 'react'
import { type components } from '@tginternal/client'
import {
  applyTolgeeTheme,
  createTolgeeApp,
  createTolgeeAppClient,
  type TolgeeApp,
  type TolgeeAppContext,
  type TolgeeAppTheme,
} from '@tolgee/apps-sdk/browser'
import './App.css'

type LanguageModel = components['schemas']['LanguageModel']

function ToolsPanelEmpty() {
  const [context, setContext] = useState<TolgeeAppContext | null>(null)
  const [selectedLanguages, setSelectedLanguages] = useState<string[]>([])
  const [theme, setTheme] = useState<TolgeeAppTheme | null>(null)
  const [languages, setLanguages] = useState<LanguageModel[] | null>(null)
  const [error, setError] = useState<string | null>(null)
  const containerRef = useRef<HTMLDivElement | null>(null)
  const appRef = useRef<TolgeeApp | null>(null)

  useEffect(() => {
    const app = createTolgeeApp()
    appRef.current = app
    app.context.then((ctx) => {
      setContext(ctx)
      setSelectedLanguages(ctx.selection.selectedLanguages ?? [])
    })
    // Re-fires when the user changes the language selector.
    const off = app.onSelectionChanged((s) =>
      setSelectedLanguages(s.selectedLanguages ?? [])
    )
    // Fires once with the initial theme, then on every light/dark toggle.
    const offTheme = app.onThemeChanged((t) => {
      applyTolgeeTheme(t)
      setTheme(t)
    })
    return () => {
      off()
      offTheme()
      app.dispose()
      appRef.current = null
    }
  }, [])

  // Fetch the full language list once we have a context, then show the details
  // (name, flag, base) for whichever tags are currently selected.
  useEffect(() => {
    if (!context) return
    const client = createTolgeeAppClient(context)
    let cancelled = false
    ;(async () => {
      setError(null)
      const resp = await client.GET('/v2/projects/{projectId}/languages', {
        params: {
          path: { projectId: context.projectId },
          query: { size: 1000 },
        },
      })
      if (cancelled) return
      if (resp.error) {
        setError(JSON.stringify(resp.error).slice(0, 200))
        return
      }
      setLanguages(resp.data?._embedded?.languages ?? [])
    })()
    return () => {
      cancelled = true
    }
  }, [context])

  useEffect(() => {
    const el = containerRef.current
    if (!el) return
    const observer = new ResizeObserver(() => {
      appRef.current?.resize(el.scrollHeight)
    })
    observer.observe(el)
    return () => observer.disconnect()
  }, [])

  const selectedSet = new Set(selectedLanguages)
  const shown = (languages ?? []).filter((l) => selectedSet.has(l.tag))

  const renderBody = () => {
    if (error) return <div className="tools-panel-hint">Error: {error}</div>
    if (selectedLanguages.length === 0)
      return <div className="tools-panel-hint">No languages selected.</div>
    if (languages == null)
      return <div className="tools-panel-hint">Loading languages…</div>
    return (
      <ul style={{ margin: 0, paddingLeft: 18 }}>
        {shown.map((l) => (
          <li key={l.tag}>
            <span>{l.flagEmoji ?? '🏳️'}</span> <strong>{l.name}</strong>{' '}
            <code>{l.tag}</code>
            {l.base ? ' · base' : ''}
          </li>
        ))}
      </ul>
    )
  }

  return (
    <div
      ref={containerRef}
      className="tools-panel"
      style={{
        background: 'var(--tg-color-background-paper)',
        color: 'var(--tg-color-text)',
      }}
    >
      <div className="tools-panel-label">Theme: {theme?.mode ?? '…'}</div>
      {theme && (
        <div style={{ display: 'flex', gap: 4, marginBottom: 10 }}>
          {Object.entries(theme.colors).map(([name, color]) => (
            <span
              key={name}
              title={`${name}: ${color}`}
              style={{
                width: 16,
                height: 16,
                borderRadius: 3,
                background: color,
                border: '1px solid var(--tg-color-divider)',
              }}
            />
          ))}
        </div>
      )}
      <div className="tools-panel-label">
        Selected languages ({selectedLanguages.length})
      </div>
      {renderBody()}
    </div>
  )
}

export default ToolsPanelEmpty
