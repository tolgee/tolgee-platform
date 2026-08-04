import { useEffect, useRef, useState } from 'react'
import {
  applyTolgeeTheme,
  createTolgeeApp,
  type TolgeeAppContext,
} from '@tolgee/apps-sdk/browser'
import {
  KEY_LIMIT,
  useProjectKeys,
  type ProjectKeys,
  type ShowcaseKey,
} from './useProjectKeys'

export const KeysShowcase = () => {
  const [context, setContext] = useState<TolgeeAppContext | null>(null)
  const pageRef = useRef<HTMLElement | null>(null)
  const state = useProjectKeys(context)

  useEffect(() => {
    const app = createTolgeeApp()

    app.context.then((ctx) => {
      applyTolgeeTheme(ctx.theme)
      setContext(ctx)
    })
    const unsubscribeTheme = app.onThemeChanged(applyTolgeeTheme)

    const page = pageRef.current
    const observer = new ResizeObserver(() => app.resize(page?.scrollHeight ?? 0))
    if (page) observer.observe(page)

    return () => {
      observer.disconnect()
      unsubscribeTheme()
      app.dispose()
    }
  }, [])

  return (
    <main className="ks-page" ref={pageRef}>
      <h1 className="ks-title">Keys Showcase</h1>
      <p className="ks-description">
        This is an example of an app showing some localization keys.
      </p>

      {state.status === 'loading' && (
        <p className="ks-muted">Loading keys from this project…</p>
      )}

      {state.status === 'forbidden' && (
        <div className="ks-notice ks-notice-error">
          Tolgee refused the request. This app was granted scopes that don’t
          include <code>keys.view</code> — re-enable it for this project so it
          can read keys.
        </div>
      )}

      {state.status === 'error' && (
        <div className="ks-notice ks-notice-error">
          Could not load keys. {state.message}
        </div>
      )}

      {state.status === 'ready' && <KeyList {...state} />}
    </main>
  )
}

const KeyList = ({ keys, baseLanguageTag, total }: ProjectKeys) => {
  if (keys.length === 0) {
    return (
      <div className="ks-notice">
        This project has no keys yet. Add one in the Translations view and it
        will show up here.
      </div>
    )
  }

  return (
    <>
      <p className="ks-muted">
        {total > KEY_LIMIT
          ? `Showing the first ${keys.length} of ${total} keys.`
          : `This project has ${total} ${total === 1 ? 'key' : 'keys'} — showing all of them.`}
      </p>
      <table className="ks-table">
        <thead>
          <tr>
            <th>Key</th>
            <th>Namespace</th>
            <th>{baseLanguageTag ?? 'Base language'}</th>
          </tr>
        </thead>
        <tbody>
          {keys.map((key) => (
            <KeyRow key={key.id} showcaseKey={key} />
          ))}
        </tbody>
      </table>
    </>
  )
}

const KeyRow = ({ showcaseKey }: { showcaseKey: ShowcaseKey }) => (
  <tr>
    <td>
      <code className="ks-key-name">{showcaseKey.name}</code>
    </td>
    <td>
      {showcaseKey.namespace ? (
        <span className="ks-badge">{showcaseKey.namespace}</span>
      ) : (
        <span className="ks-muted">—</span>
      )}
    </td>
    <td>
      {showcaseKey.baseTranslation ?? (
        <span className="ks-muted">Not translated</span>
      )}
    </td>
  </tr>
)
