import { useCallback, useEffect, useMemo, useState } from 'react'
import { type components } from '@tginternal/client'
import {
  applyTolgeeTheme,
  createTolgeeApp,
  createTolgeeAppClient,
  type TolgeeAppContext,
  type TolgeeAppSelection,
} from '@tolgee/apps-sdk/browser'
import './App.css'

type KeyRow = components['schemas']['KeyWithTranslationsModel']
type LanguageModel = components['schemas']['LanguageModel']

type PluginState = {
  emojis: Record<string, string>
  updatedAt: Record<string, string>
  events: Record<string, string[]>
}

const EMPTY_SELECTION: TolgeeAppSelection = {}

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

function KeyEditTab() {
  const [context, setContext] = useState<TolgeeAppContext | null>(null)
  const [selection, setSelection] = useState<TolgeeAppSelection>(EMPTY_SELECTION)
  const [row, setRow] = useState<KeyRow | null>(null)
  const [languages, setLanguages] = useState<LanguageModel[]>([])
  const [pluginState, setPluginState] = useState<PluginState>({
    emojis: {},
    updatedAt: {},
    events: {},
  })
  const [error, setError] = useState<string | null>(null)

  useEffect(() => {
    const app = createTolgeeApp()
    app.context.then((ctx) => {
      setContext(ctx)
      setSelection(ctx.selection)
    })
    const off = app.onSelectionChanged(setSelection)
    const offTheme = app.onThemeChanged(applyTolgeeTheme)
    return () => {
      off()
      offTheme()
      app.dispose()
    }
  }, [])

  useEffect(() => {
    if (!context || selection.keyId == null) return
    const client = createTolgeeAppClient(context)
    let cancelled = false
    ;(async () => {
      try {
        const resp = await client.GET(
          '/v2/projects/{projectId}/translations',
          {
            params: {
              path: { projectId: context.projectId },
              query: { filterKeyId: [selection.keyId!], size: 1 },
            },
          }
        )
        if (cancelled) return
        if (resp.error) {
          setError(`Translations lookup failed: ${JSON.stringify(resp.error).slice(0, 200)}`)
          return
        }
        setRow(resp.data?._embedded?.keys?.[0] ?? null)
        setLanguages(resp.data?.selectedLanguages ?? [])
      } catch (e) {
        if (!cancelled) setError(e instanceof Error ? e.message : String(e))
      }
    })()
    return () => {
      cancelled = true
    }
  }, [context, selection.keyId])

  const translationIds = useMemo(() => {
    if (!row) return [] as number[]
    const ids: number[] = []
    for (const t of Object.values(row.translations)) {
      if (t?.id != null) ids.push(t.id)
    }
    return ids
  }, [row])

  const idsKey = translationIds.join(',')

  useEffect(() => {
    if (!idsKey) {
      setPluginState({ emojis: {}, updatedAt: {}, events: {} })
      return
    }
    const ctrl = new AbortController()
    fetch(`/api/state?ids=${idsKey}`, { signal: ctrl.signal })
      .then((r) => (r.ok ? r.json() : null))
      .then((data: PluginState | null) => {
        if (data) setPluginState(data)
      })
      .catch((err) => {
        if (err.name !== 'AbortError') console.warn('plugin state fetch:', err)
      })
    return () => ctrl.abort()
  }, [idsKey])

  const sorted = useCallback(
    (langs: LanguageModel[]): LanguageModel[] =>
      [...langs].sort((a, b) => a.tag.localeCompare(b.tag)),
    []
  )

  if (!context) {
    return (
      <main className="key-edit-tab">
        <p>Waiting for context from the host…</p>
      </main>
    )
  }

  if (error) {
    return (
      <main className="key-edit-tab">
        <p style={{ color: '#b00' }}>{error}</p>
      </main>
    )
  }

  if (!row || selection.keyId == null) {
    return (
      <main className="key-edit-tab">
        <p>No key selected.</p>
      </main>
    )
  }

  return (
    <main className="key-edit-tab">
      <h2 className="key-edit-tab-title">
        🛡️ Audit for{' '}
        {row.keyNamespace && <code className="ns">{row.keyNamespace}</code>}
        <strong>{row.keyName}</strong>
      </h2>
      <table className="audit">
        <thead>
          <tr>
            <th>Language</th>
            <th>Last edit</th>
            <th>Edits (14d)</th>
          </tr>
        </thead>
        <tbody>
          {sorted(languages).map((lang) => {
            const t = row.translations[lang.tag]
            const id = t?.id != null ? String(t.id) : null
            const lastEdit = id ? pluginState.updatedAt[id] : null
            const events = id ? pluginState.events[id]?.length ?? 0 : 0
            return (
              <tr key={lang.tag}>
                <td>
                  <code className="lang">{lang.tag}</code> {lang.name}
                </td>
                <td>{lastEdit ? formatRelative(lastEdit) : <em>—</em>}</td>
                <td>{events}</td>
              </tr>
            )
          })}
        </tbody>
      </table>
    </main>
  )
}

export default KeyEditTab
