import { useCallback, useEffect, useMemo, useState } from 'react'
import { type components } from '@tginternal/client'
import {
  applyTolgeeTheme,
  createTolgeeApp,
  createTolgeeAppClient,
  type TolgeeAppContext,
} from '@tolgee/apps-sdk/browser'
import './App.css'

type KeyRow = components['schemas']['KeyWithTranslationsModel']
type LanguageModel = components['schemas']['LanguageModel']

type PluginState = {
  emojis: Record<string, string>
  updatedAt: Record<string, string>
  events: Record<string, string[]>
}

const EMOJI_PALETTE = ['🎉', '🔥', '❓', '✅', '❌'] as const
const SPARKLINE_DAYS = 14
const SPARKLINE_WIDTH = 84
const SPARKLINE_HEIGHT = 18
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

const formatRelative = (iso: string): string => {
  const t = new Date(iso).getTime()
  if (Number.isNaN(t)) return ''
  const diff = Date.now() - t
  const sec = Math.round(diff / 1000)
  if (sec < 60) return `${sec}s ago`
  const min = Math.round(sec / 60)
  if (min < 60) return `${min}m ago`
  const hr = Math.round(min / 60)
  if (hr < 24) return `${hr}h ago`
  const day = Math.round(hr / 24)
  return `${day}d ago`
}

function App() {
  const [context, setContext] = useState<TolgeeAppContext | null>(null)
  const [error, setError] = useState<string | null>(null)
  const [loading, setLoading] = useState(true)
  const [keys, setKeys] = useState<KeyRow[]>([])
  const [languages, setLanguages] = useState<LanguageModel[]>([])
  const [projectName, setProjectName] = useState<string | null>(null)
  const [pluginState, setPluginState] = useState<PluginState>({
    emojis: {},
    updatedAt: {},
    events: {},
  })

  useEffect(() => {
    const app = createTolgeeApp()
    app.context.then(setContext)
    const offTheme = app.onThemeChanged(applyTolgeeTheme)
    return () => {
      offTheme()
      app.dispose()
    }
  }, [])

  useEffect(() => {
    if (!context) return

    const client = createTolgeeAppClient(context)

    let cancelled = false
    ;(async () => {
      setLoading(true)
      setError(null)
      try {
        const projectResp = await client.GET('/v2/projects/{projectId}', {
          params: { path: { projectId: context.projectId } },
        })
        if (projectResp.error) {
          throw new Error(
            `Project lookup failed: ${JSON.stringify(projectResp.error).slice(
              0,
              240
            )}`
          )
        }
        if (cancelled) return
        if (projectResp.data) setProjectName(projectResp.data.name)

        const keysResp = await client.GET(
          '/v2/projects/{projectId}/translations',
          {
            params: {
              path: { projectId: context.projectId },
              query: { size: 20 },
            },
          }
        )
        if (cancelled) return
        if (keysResp.error) {
          throw new Error(
            `Translations lookup failed: ${JSON.stringify(keysResp.error).slice(
              0,
              240
            )}`
          )
        }
        const loadedKeys = keysResp.data?._embedded?.keys ?? []
        const loadedLanguages = keysResp.data?.selectedLanguages ?? []
        setKeys(loadedKeys)
        setLanguages(loadedLanguages)
      } catch (e) {
        if (!cancelled) {
          setError(e instanceof Error ? e.message : String(e))
        }
      } finally {
        if (!cancelled) setLoading(false)
      }
    })()

    return () => {
      cancelled = true
    }
  }, [context])

  const noteTranslationIdsKey = useMemo(() => {
    const ids: number[] = []
    for (const row of keys) {
      for (const t of Object.values(row.translations)) {
        if (t?.id != null) ids.push(t.id)
      }
    }
    return ids.join(',')
  }, [keys])

  useEffect(() => {
    if (!noteTranslationIdsKey) return
    const ctrl = new AbortController()
    fetch(`/api/state?ids=${noteTranslationIdsKey}`, {
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
  }, [noteTranslationIdsKey])

  const noteTranslationIdFor = useCallback(
    (row: KeyRow): number | null => {
      for (const lang of languages) {
        const t = row.translations[lang.tag]
        if (t?.id != null) return t.id
      }
      return null
    },
    [languages]
  )

  const setEmoji = useCallback(
    async (translationId: number, emoji: string | null) => {
      setPluginState((prev) => {
        const emojis = { ...prev.emojis }
        if (emoji) emojis[translationId] = emoji
        else delete emojis[translationId]
        return { ...prev, emojis }
      })
      try {
        await fetch('/api/emoji', {
          method: 'PUT',
          headers: { 'Content-Type': 'application/json' },
          body: JSON.stringify({ translationId, emoji }),
        })
      } catch (err) {
        console.warn('save emoji:', err)
      }
    },
    []
  )

  return (
    <main className="plugin-page">
      <h1>
        Hello World 👋
        {projectName && (
          <small style={{ marginLeft: 8, color: '#666' }}>
            from {projectName}
          </small>
        )}
      </h1>

      {!context && <p>Waiting for context from the host…</p>}
      {context && loading && <p>Loading…</p>}
      {error && (
        <p style={{ color: '#b00' }} data-testid="error">
          {error}
        </p>
      )}

      {context && !loading && !error && (
        <>
          <p>
            This is the Tolgee dev plugin. It used a signed app token to fetch
            the first {keys.length} keys in this project via the REST API.
          </p>

          {keys.length === 0 ? (
            <p>No keys in this project yet.</p>
          ) : (
            <table className="keys">
              <thead>
                <tr>
                  <th>Key</th>
                  <th>Note</th>
                  <th>
                    Activity
                    <span className="th-hint">last {SPARKLINE_DAYS}d</span>
                  </th>
                  {languages.map((lang) => (
                    <th key={lang.tag}>
                      <code className="lang">{lang.tag}</code> {lang.name}
                    </th>
                  ))}
                </tr>
              </thead>
              <tbody>
                {keys.map((row) => {
                  const noteId = noteTranslationIdFor(row)
                  const currentEmoji =
                    noteId != null
                      ? pluginState.emojis[String(noteId)] ?? null
                      : null
                  const lastUpdated =
                    noteId != null
                      ? pluginState.updatedAt[String(noteId)] ?? null
                      : null
                  const rowEvents: string[] = []
                  for (const t of Object.values(row.translations)) {
                    if (t?.id != null) {
                      const log = pluginState.events[String(t.id)]
                      if (log) rowEvents.push(...log)
                    }
                  }
                  const buckets = bucketEvents(rowEvents, SPARKLINE_DAYS)
                  const totalEdits = buckets.reduce((sum, c) => sum + c, 0)
                  return (
                    <tr key={row.keyId}>
                      <td>
                        {row.keyNamespace && (
                          <code className="ns">{row.keyNamespace}</code>
                        )}
                        <strong>{row.keyName}</strong>
                      </td>
                      <td>
                        <div className="emoji-row">
                          {EMOJI_PALETTE.map((e) => (
                            <button
                              key={e}
                              type="button"
                              className={`emoji-btn${
                                currentEmoji === e ? ' selected' : ''
                              }`}
                              onClick={() =>
                                noteId != null &&
                                setEmoji(noteId, currentEmoji === e ? null : e)
                              }
                              disabled={noteId == null}
                              aria-pressed={currentEmoji === e}
                            >
                              {e}
                            </button>
                          ))}
                          <button
                            type="button"
                            className="emoji-btn clear"
                            onClick={() =>
                              noteId != null && setEmoji(noteId, null)
                            }
                            disabled={noteId == null || !currentEmoji}
                            aria-label="Clear"
                          >
                            ×
                          </button>
                        </div>
                        <div className="updated">
                          {lastUpdated
                            ? `Updated ${formatRelative(lastUpdated)}`
                            : '—'}
                        </div>
                      </td>
                      <td className="activity-cell">
                        <Sparkline buckets={buckets} />
                        <div className="activity-count">
                          {totalEdits} {totalEdits === 1 ? 'edit' : 'edits'}
                        </div>
                      </td>
                      {languages.map((lang) => {
                        const t = row.translations[lang.tag]
                        return (
                          <td key={lang.tag}>{t?.text ?? <em>—</em>}</td>
                        )
                      })}
                    </tr>
                  )
                })}
              </tbody>
            </table>
          )}
        </>
      )}
    </main>
  )
}

export default App
