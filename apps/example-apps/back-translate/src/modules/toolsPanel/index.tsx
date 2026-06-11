import { useEffect, useRef, useState } from 'react'
import {
  applyTolgeeTheme,
  createTolgeeApp,
  createTolgeeAppClient,
  type TolgeeApp,
  type TolgeeAppContext,
  type TolgeeAppSelection,
} from '@tolgee/apps-sdk/browser'

type Verdict = 'match' | 'close' | 'mismatch'

type Analysis = {
  backTranslation: string
  verdict: Verdict
  explanation: string
  cached?: boolean
}

type Pair = {
  baseText: string
  text: string
  sourceLang: string
  keyId: number
  languageTag: string
}

const VERDICT: Record<Verdict, { glyph: string; color: string; label: string }> = {
  match: { glyph: '✓', color: '#1b8c43', label: 'Semantic match' },
  close: { glyph: '~', color: '#c98a14', label: 'Close, minor drift' },
  mismatch: { glyph: '✗', color: '#c44343', label: 'Semantic mismatch' },
}

export default function ToolsPanel() {
  const [ctx, setCtx] = useState<TolgeeAppContext | null>(null)
  const [selection, setSelection] = useState<TolgeeAppSelection>({})
  const [baseLang, setBaseLang] = useState<string | null>(null)
  const [pair, setPair] = useState<Pair | null>(null)
  const [analysis, setAnalysis] = useState<Analysis | null>(null)
  const [loading, setLoading] = useState(false)
  const [error, setError] = useState<string | null>(null)

  const containerRef = useRef<HTMLDivElement | null>(null)
  const appRef = useRef<TolgeeApp | null>(null)

  // Boot the SDK handshake.
  useEffect(() => {
    const app = createTolgeeApp()
    appRef.current = app
    app.context.then((c) => {
      setCtx(c)
      setSelection(c.selection)
    })
    const off = app.onSelectionChanged(setSelection)
    const offTheme = app.onThemeChanged(applyTolgeeTheme)
    return () => {
      off()
      offTheme()
      app.dispose()
      appRef.current = null
    }
  }, [])

  // Resize the iframe to fit its content.
  useEffect(() => {
    const el = containerRef.current
    if (!el) return
    const observer = new ResizeObserver(() => {
      appRef.current?.resize(el.scrollHeight)
    })
    observer.observe(el)
    return () => observer.disconnect()
  }, [])

  // Discover the project's base language once.
  useEffect(() => {
    if (!ctx) return
    const client = createTolgeeAppClient(ctx)
    let cancelled = false
    client
      .GET('/v2/projects/{projectId}', {
        params: { path: { projectId: ctx.projectId } },
      })
      .then(({ data, error }) => {
        if (cancelled) return
        if (error) setError(`Project lookup failed: ${JSON.stringify(error)}`)
        else if (data?.baseLanguage?.tag) setBaseLang(data.baseLanguage.tag)
      })
    return () => {
      cancelled = true
    }
  }, [ctx])

  // Fetch the base + focused texts whenever the selection changes.
  useEffect(() => {
    if (!ctx || !baseLang) return
    const tag = selection.languageTag
    const keyId = selection.keyId
    if (!keyId || !tag || tag === baseLang) {
      setPair(null)
      setAnalysis(null)
      setError(null)
      return
    }
    const client = createTolgeeAppClient(ctx)
    let cancelled = false
    setError(null)
    client
      .GET('/v2/projects/{projectId}/translations', {
        params: {
          path: { projectId: ctx.projectId },
          query: {
            filterKeyId: [keyId],
            languages: [baseLang, tag],
            size: 1,
          },
        },
      })
      .then(({ data, error }) => {
        if (cancelled) return
        if (error) {
          setError(`Translation lookup failed: ${JSON.stringify(error)}`)
          return
        }
        const row = data?._embedded?.keys?.[0]
        const baseText = row?.translations?.[baseLang]?.text
        const text = row?.translations?.[tag]?.text
        if (!baseText || !text) {
          setPair(null)
          return
        }
        setPair({ baseText, text, sourceLang: tag, keyId, languageTag: tag })
      })
    return () => {
      cancelled = true
    }
  }, [ctx, baseLang, selection.keyId, selection.languageTag])

  // Whenever we have a fresh pair, ask Claude.
  useEffect(() => {
    if (!pair || !baseLang) return
    let cancelled = false
    setLoading(true)
    setError(null)
    setAnalysis(null)
    fetch('/translate-back', {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({ ...pair, baseLang }),
    })
      .then(async (r) => {
        const body = await r.text()
        if (!r.ok) throw new Error(`${r.status}: ${body}`)
        return JSON.parse(body) as Analysis
      })
      .then((result) => {
        if (cancelled) return
        setAnalysis(result)
        setLoading(false)
      })
      .catch((e) => {
        if (cancelled) return
        setError(e instanceof Error ? e.message : String(e))
        setLoading(false)
      })
    return () => {
      cancelled = true
    }
  }, [pair, baseLang])

  return (
    <main ref={containerRef} className="back-translate-panel">
      {!ctx && <p className="bt-status">Loading context…</p>}
      {ctx && !baseLang && !error && (
        <p className="bt-status">Loading project metadata…</p>
      )}
      {ctx && baseLang && (
        <>
          {!selection.keyId && (
            <p className="bt-status">Focus a translation cell to analyse it.</p>
          )}
          {selection.keyId && selection.languageTag === baseLang && (
            <p className="bt-status">
              Focused cell is in the base language ({baseLang}); nothing to
              back-translate.
            </p>
          )}
          {selection.keyId &&
            selection.languageTag &&
            selection.languageTag !== baseLang &&
            !pair &&
            !error && <p className="bt-status">Loading translation…</p>}
          {pair && (
            <>
              {loading && (
                <p className="bt-status">Back-translating with Claude…</p>
              )}
              {analysis && (
                <>
                  <Row
                    label={`${baseLang} (back)`}
                    value={analysis.backTranslation}
                  />
                  <VerdictBadge
                    verdict={analysis.verdict}
                    explanation={analysis.explanation}
                    cached={analysis.cached}
                  />
                </>
              )}
            </>
          )}
        </>
      )}
      {error && <p className="bt-error">{error}</p>}
    </main>
  )
}

const Row = ({
  label,
  value,
  muted,
}: {
  label: string
  value: string
  muted?: boolean
}) => (
  <div className={`bt-row${muted ? ' bt-row-muted' : ''}`}>
    <div className="bt-row-label">{label}</div>
    <div className="bt-row-value">{value}</div>
  </div>
)

const VerdictBadge = ({
  verdict,
  explanation,
  cached,
}: {
  verdict: Verdict
  explanation: string
  cached?: boolean
}) => {
  const v = VERDICT[verdict] ?? {
    glyph: '?',
    color: '#888',
    label: 'Unknown',
  }
  return (
    <div className="bt-verdict" style={{ borderColor: v.color }}>
      <span className="bt-verdict-glyph" style={{ color: v.color }}>
        {v.glyph}
      </span>
      <div>
        <strong style={{ color: v.color }}>{v.label}</strong>
        {cached && <span className="bt-cached"> · cached</span>}
        <div className="bt-verdict-explanation">{explanation}</div>
      </div>
    </div>
  )
}
