import { useCallback, useEffect, useRef, useState } from 'react'
import {
  applyTolgeeTheme,
  createTolgeeApp,
  type TolgeeApp,
  type TolgeeAppContext,
} from '@tolgee/apps-sdk/browser'
import './../../App.css'

type Glossary = { id: number; name: string; baseLanguageTag: string }

type Suggestion = {
  id: string
  keyName: string
  languageTag: string
  term: string
  translation: string
  description: string
}

type GlossariesState =
  | { status: 'loading' }
  | { status: 'unavailable' }
  | { status: 'ready'; glossaries: Glossary[] }

export default function Dashboard() {
  const [ctx, setCtx] = useState<TolgeeAppContext | null>(null)
  const [glossaries, setGlossaries] = useState<GlossariesState>({ status: 'loading' })
  const [selectedGlossaryId, setSelectedGlossaryId] = useState<number | null>(null)
  const [suggestions, setSuggestions] = useState<Suggestion[]>([])
  const [busy, setBusy] = useState(false)
  const containerRef = useRef<HTMLDivElement | null>(null)
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

  // Discover the project's glossaries (none / one / many) via the app token.
  useEffect(() => {
    if (!ctx) return
    const ctrl = new AbortController()
    fetch(`${ctx.apiUrl}/v2/projects/${ctx.projectId}/glossaries`, {
      headers: { Authorization: `Bearer ${ctx.token}` },
      signal: ctrl.signal,
    })
      .then((r) => (r.ok ? r.json() : Promise.reject(new Error(String(r.status)))))
      .then((data: { _embedded?: { glossaries?: Glossary[] } }) => {
        const list = data._embedded?.glossaries ?? []
        setGlossaries({ status: 'ready', glossaries: list })
        setSelectedGlossaryId((cur) => cur ?? list[0]?.id ?? null)
      })
      .catch((err) => {
        if (err.name === 'AbortError') return
        setGlossaries({ status: 'unavailable' })
      })
    return () => ctrl.abort()
  }, [ctx])

  const loadSuggestions = useCallback(() => {
    if (!ctx) return
    fetch(`/suggestions?projectId=${ctx.projectId}`)
      .then((r) => (r.ok ? r.json() : { suggestions: [] }))
      .then((data: { suggestions?: Suggestion[] }) => setSuggestions(data.suggestions ?? []))
      .catch(() => setSuggestions([]))
  }, [ctx])

  useEffect(() => loadSuggestions(), [loadSuggestions])

  // Keep the iframe sized to its content.
  useEffect(() => {
    const el = containerRef.current
    if (!el) return
    const observer = new ResizeObserver(() => appRef.current?.resize(el.scrollHeight))
    observer.observe(el)
    return () => observer.disconnect()
  }, [])

  const accept = async (suggestionId: string) => {
    if (selectedGlossaryId == null) return
    setBusy(true)
    try {
      const res = await fetch('/accept', {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({ suggestionId, glossaryId: selectedGlossaryId }),
      })
      if (res.ok) setSuggestions((cur) => cur.filter((s) => s.id !== suggestionId))
    } finally {
      setBusy(false)
    }
  }

  const acceptAll = async () => {
    if (selectedGlossaryId == null || ctx == null) return
    setBusy(true)
    try {
      await fetch('/accept-all', {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({ projectId: ctx.projectId, glossaryId: selectedGlossaryId }),
      })
      loadSuggestions()
    } finally {
      setBusy(false)
    }
  }

  return (
    <main ref={containerRef} className="gk-page">
      <h1>Glossary updates</h1>
      <p className="gk-subtitle">
        Suggested glossary entries from recent translation changes.
      </p>

      {glossaries.status === 'loading' && <p className="gk-muted">Loading glossaries…</p>}

      {(glossaries.status === 'unavailable' ||
        (glossaries.status === 'ready' && glossaries.glossaries.length === 0)) && (
        <div className="gk-info">
          This project has no glossary yet. Create a glossary and assign it to this project,
          then suggestions can be accepted into it.
        </div>
      )}

      {glossaries.status === 'ready' && glossaries.glossaries.length > 0 && (
        <>
          <div className="gk-toolbar">
            <label className="gk-field">
              Target glossary
              <select
                value={selectedGlossaryId ?? ''}
                onChange={(e) => setSelectedGlossaryId(Number(e.target.value))}
              >
                {glossaries.glossaries.map((g) => (
                  <option key={g.id} value={g.id}>
                    {g.name} ({g.baseLanguageTag})
                  </option>
                ))}
              </select>
            </label>
            <button
              className="gk-btn gk-btn-primary"
              disabled={busy || suggestions.length === 0}
              onClick={acceptAll}
            >
              Accept all
            </button>
          </div>

          {suggestions.length === 0 ? (
            <p className="gk-muted">No pending suggestions. Edit a translation to generate some.</p>
          ) : (
            <table className="gk-table">
              <thead>
                <tr>
                  <th>Term</th>
                  <th>Translation</th>
                  <th>Key</th>
                  <th />
                </tr>
              </thead>
              <tbody>
                {suggestions.map((s) => (
                  <tr key={s.id}>
                    <td>
                      <div className="gk-term">{s.term}</div>
                      <div className="gk-desc">{s.description}</div>
                    </td>
                    <td>
                      <span className="gk-lang">{s.languageTag}</span> {s.translation}
                    </td>
                    <td>
                      <code>{s.keyName}</code>
                    </td>
                    <td>
                      <button className="gk-btn" disabled={busy} onClick={() => accept(s.id)}>
                        Accept
                      </button>
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
