import { useEffect, useRef, useState } from 'react'
import {
  applyTolgeeTheme,
  createTolgeeApp,
  type TolgeeAppContext,
} from '@tolgee/apps-sdk/browser'
import type { ProjectFeed, TranslationChange, WorkerStatus } from './feedTypes'
import { REFRESH_INTERVAL_MS, useActivityFeed } from './useActivityFeed'

export const ActivityWorker = () => {
  const [context, setContext] = useState<TolgeeAppContext | null>(null)
  const pageRef = useRef<HTMLElement | null>(null)
  const state = useActivityFeed(context)

  useEffect(() => {
    // Set from TOLGEE_URL at build time (see vite.config.ts). Empty when the app
    // is built without one — the SDK then falls back to pinning whichever origin
    // completes the handshake first.
    const app = createTolgeeApp({
      tolgeeOrigin: import.meta.env.VITE_TOLGEE_ORIGIN || undefined,
    })

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
    <main className="aw-page" ref={pageRef}>
      <h1 className="aw-title">Activity Worker</h1>
      <p className="aw-description">
        This app’s backend polls Tolgee on its own — no browser involved. It asks
        Tolgee which projects it is enabled for, watches their activity, and
        keeps the translation changes it sees. This page just shows what the
        backend has collected for this project, refreshed every{' '}
        {REFRESH_INTERVAL_MS / 1000}s.
      </p>

      {state.status === 'loading' && (
        <p className="aw-muted">Loading the worker’s feed…</p>
      )}

      {state.status === 'forbidden' && (
        <div className="aw-notice aw-notice-error">
          Tolgee refused this token for the project. This app was granted scopes
          that don’t include <code>activity.view</code> — re-enable it for this
          project so it can read activity.
        </div>
      )}

      {state.status === 'error' && (
        <div className="aw-notice aw-notice-error">
          Could not reach the app’s backend. {state.message}
        </div>
      )}

      {state.status === 'ready' && (
        <>
          <WorkerSummary worker={state.worker} />
          <FeedBody feed={state.feed} />
        </>
      )}
    </main>
  )
}

const WorkerSummary = ({ worker }: { worker: WorkerStatus }) => {
  if (!worker.connected) {
    return (
      <p className="aw-muted">
        The worker has not reached Tolgee yet
        {worker.lastError ? `: ${worker.lastError}` : '.'}
      </p>
    )
  }

  return (
    <p className="aw-muted">
      Watching {worker.watchedProjectCount} project
      {worker.watchedProjectCount === 1 ? '' : 's'} across {worker.installCount}{' '}
      install{worker.installCount === 1 ? '' : 's'}, polling activity every{' '}
      {worker.activityPollIntervalMs / 1000}s and re-checking installations
      every {worker.installationsRefreshIntervalMs / 1000}s.
    </p>
  )
}

const FeedBody = ({ feed }: { feed: ProjectFeed | null }) => {
  if (!feed) {
    return (
      <div className="aw-notice">
        The worker is not watching this project yet. It re-checks its
        installations periodically — this page will fill in once it does.
      </div>
    )
  }

  if (feed.lastError) {
    return (
      <div className="aw-notice aw-notice-error">
        The last poll of {feed.projectName} failed: {feed.lastError}
      </div>
    )
  }

  if (feed.changes.length === 0) {
    return (
      <div className="aw-notice">
        No translation changes seen in {feed.projectName} yet. Edit a translation
        and it will appear here.
      </div>
    )
  }

  return (
    <>
      <p className="aw-muted">
        {feed.changes.length} translation change
        {feed.changes.length === 1 ? '' : 's'} in {feed.organizationSlug}/
        {feed.projectName}
        {feed.lastPolledAt
          ? `, last polled ${new Date(feed.lastPolledAt).toLocaleTimeString()}`
          : ''}
        .
      </p>
      <ul className="aw-feed">
        {feed.changes.map((change) => (
          <ChangeRow
            key={`${change.revisionId}-${change.keyName}-${change.languageTag}`}
            change={change}
            projectName={feed.projectName}
          />
        ))}
      </ul>
    </>
  )
}

const ChangeRow = ({
  change,
  projectName,
}: {
  change: TranslationChange
  projectName: string
}) => (
  <li className="aw-feed-item">
    <div className="aw-feed-headline">
      <code className="aw-key-name">
        {change.namespace ? `${change.namespace}:` : ''}
        {change.keyName ?? 'unknown key'}
      </code>
      {change.languageTag && (
        <span className="aw-badge">{change.languageTag}</span>
      )}
      <span className="aw-muted">
        changed in {projectName} at{' '}
        {new Date(change.timestamp).toLocaleString()}
      </span>
    </div>
    <div className="aw-feed-diff">
      <span className="aw-old">{change.oldText ?? '(empty)'}</span>
      <span className="aw-arrow">→</span>
      <span className="aw-new">{change.newText ?? '(empty)'}</span>
    </div>
    <div className="aw-muted aw-feed-meta">
      {change.authorName ?? 'unknown author'} · {change.activityType} ·
      revision&nbsp;{change.revisionId}
    </div>
  </li>
)
