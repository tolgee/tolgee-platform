import { useEffect, useMemo, useRef, useState } from 'react'
import {
  applyTolgeeTheme,
  createTolgeeApp,
  type TolgeeApp,
  type TolgeeAppSelection,
} from '@tolgee/apps-sdk/browser'
import './App.css'

type PluginState = {
  emojis: Record<string, string>
  updatedAt: Record<string, string>
  events: Record<string, string[]>
}

const EMPTY_SELECTION: TolgeeAppSelection = {}

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
            fill={
              count === 0
                ? 'var(--tg-color-divider, #ddd)'
                : 'var(--tg-color-primary, #aa3bff)'
            }
            rx={1}
          />
        )
      })}
    </svg>
  )
}

function ToolsPanel() {
  const [selection, setSelection] = useState<TolgeeAppSelection>(EMPTY_SELECTION)
  const [pluginState, setPluginState] = useState<PluginState | null>(null)
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
    const el = containerRef.current
    if (!el) return
    const observer = new ResizeObserver(() => {
      appRef.current?.resize(el.scrollHeight)
    })
    observer.observe(el)
    return () => observer.disconnect()
  }, [])

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
