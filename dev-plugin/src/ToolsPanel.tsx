import { useEffect, useMemo, useRef, useState } from 'react'
import './App.css'

type InitMessage = {
  type: 'tolgee-app:init'
  token: string
  apiUrl: string
  organizationId: number
  projectId: number
  keyId: number | null
  languageId: number | null
  languageTag: string | null
  translationId: number | null
}

type SelectionMessage = {
  type: 'tolgee-app:selection-changed'
  keyId: number | null
  languageId: number | null
  languageTag: string | null
  translationId: number | null
}

type PluginState = {
  emojis: Record<string, string>
  updatedAt: Record<string, string>
  events: Record<string, string[]>
}

type Selection = {
  keyId: number | null
  languageId: number | null
  languageTag: string | null
  translationId: number | null
}

const EMPTY_SELECTION: Selection = {
  keyId: null,
  languageId: null,
  languageTag: null,
  translationId: null,
}

const SPARKLINE_DAYS = 14
const SPARKLINE_WIDTH = 168
const SPARKLINE_HEIGHT = 26
const MS_PER_DAY = 24 * 60 * 60 * 1000

const bucketEvents = (timestamps: string[], days: number): number[] => {
  const now = Date.now()
  const startOfTodayUtc = now - (now % MS_PER_DAY)
  const buckets = new Array<number>(days).fill(0)
  for (const iso of timestamps) {
    const t = Date.parse(iso)
    if (Number.isNaN(t)) continue
    const startOfEventDayUtc = t - (t % MS_PER_DAY)
    const dayOffset = Math.round(
      (startOfTodayUtc - startOfEventDayUtc) / MS_PER_DAY
    )
    if (dayOffset < 0 || dayOffset >= days) continue
    buckets[days - 1 - dayOffset]++
  }
  return buckets
}

const Sparkline = ({ buckets }: { buckets: number[] }) => {
  const max = Math.max(1, ...buckets)
  const barWidth = SPARKLINE_WIDTH / buckets.length
  return (
    <svg
      width={SPARKLINE_WIDTH}
      height={SPARKLINE_HEIGHT}
      role="img"
      aria-label={`Activity: ${buckets.reduce(
        (sum, c) => sum + c,
        0
      )} edits over ${buckets.length} days`}
    >
      {buckets.map((count, i) => {
        const h = count === 0 ? 1 : (count / max) * (SPARKLINE_HEIGHT - 1)
        return (
          <rect
            key={i}
            x={i * barWidth + 1}
            y={SPARKLINE_HEIGHT - h}
            width={Math.max(1, barWidth - 2)}
            height={h}
            fill={count === 0 ? '#ddd' : '#aa3bff'}
            rx={1}
          />
        )
      })}
    </svg>
  )
}

function ToolsPanel() {
  const [hostOrigin, setHostOrigin] = useState<string | null>(null)
  const [selection, setSelection] = useState<Selection>(EMPTY_SELECTION)
  const [pluginState, setPluginState] = useState<PluginState | null>(null)
  const containerRef = useRef<HTMLDivElement | null>(null)

  useEffect(() => {
    const handler = (event: MessageEvent) => {
      const data = event.data as InitMessage | SelectionMessage | undefined
      if (!data || typeof data !== 'object') return
      if (data.type === 'tolgee-app:init') {
        setHostOrigin(event.origin)
        setSelection({
          keyId: data.keyId,
          languageId: data.languageId,
          languageTag: data.languageTag,
          translationId: data.translationId,
        })
      } else if (data.type === 'tolgee-app:selection-changed') {
        setSelection({
          keyId: data.keyId,
          languageId: data.languageId,
          languageTag: data.languageTag,
          translationId: data.translationId,
        })
      }
    }
    window.addEventListener('message', handler)
    window.parent.postMessage({ type: 'tolgee-app:ready' }, '*')
    return () => window.removeEventListener('message', handler)
  }, [])

  useEffect(() => {
    if (selection.translationId == null) {
      setPluginState(null)
      return
    }
    const ctrl = new AbortController()
    fetch(`/api/state?ids=${selection.translationId}`, {
      signal: ctrl.signal,
    })
      .then((r) => (r.ok ? r.json() : null))
      .then((data: PluginState | null) => {
        if (data) setPluginState(data)
      })
      .catch((err) => {
        if (err.name !== 'AbortError') console.warn('plugin state fetch:', err)
      })
    return () => ctrl.abort()
  }, [selection.translationId])

  const buckets = useMemo(() => {
    if (selection.translationId == null || !pluginState) {
      return new Array<number>(SPARKLINE_DAYS).fill(0)
    }
    const events = pluginState.events[String(selection.translationId)] ?? []
    return bucketEvents(events, SPARKLINE_DAYS)
  }, [selection.translationId, pluginState])

  const totalEdits = buckets.reduce((sum, c) => sum + c, 0)

  useEffect(() => {
    if (!hostOrigin) return
    const el = containerRef.current
    if (!el) return
    const observer = new ResizeObserver(() => {
      window.parent.postMessage(
        { type: 'tolgee-app:resize', height: el.scrollHeight },
        hostOrigin
      )
    })
    observer.observe(el)
    return () => observer.disconnect()
  }, [hostOrigin])

  return (
    <div ref={containerRef} className="tools-panel">
      <div className="tools-panel-row">
        <div className="tools-panel-label">Activity (last {SPARKLINE_DAYS}d)</div>
        <Sparkline buckets={buckets} />
        <div className="tools-panel-count">
          {totalEdits} {totalEdits === 1 ? 'edit' : 'edits'}
        </div>
      </div>
      <div className="tools-panel-hint">
        {selection.translationId == null
          ? 'Focus a translation cell to see its activity.'
          : `translationId=${selection.translationId} · lang=${
              selection.languageTag ?? '—'
            }`}
      </div>
    </div>
  )
}

export default ToolsPanel
