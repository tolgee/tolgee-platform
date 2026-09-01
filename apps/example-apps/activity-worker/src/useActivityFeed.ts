import { useEffect, useState } from 'react'
import type { TolgeeAppContext } from '@tolgee/apps-sdk/browser'
import type { FeedResponse } from './feedTypes'

/** How often the page re-reads the worker's feed. */
export const REFRESH_INTERVAL_MS = 5_000

export type ActivityFeedState =
  | { status: 'loading' }
  | { status: 'forbidden' }
  | { status: 'error'; message: string }
  | ({ status: 'ready' } & FeedResponse)

/**
 * Reads the feed this app's own backend has been building — not Tolgee. The
 * page shows what the worker observed, so it must ask the worker.
 *
 * The context token goes along as a bearer token: the app server replays it
 * against Tolgee to decide whether this viewer may see the project's activity.
 */
export const useActivityFeed = (
  context: TolgeeAppContext | null
): ActivityFeedState => {
  const [state, setState] = useState<ActivityFeedState>({ status: 'loading' })

  useEffect(() => {
    if (!context) return
    const abort = new AbortController()

    const load = async (): Promise<ActivityFeedState> => {
      const response = await fetch('/api/feed', {
        headers: { Authorization: `Bearer ${context.token}` },
        signal: abort.signal,
      })

      if (response.status === 401 || response.status === 403) {
        return { status: 'forbidden' }
      }
      if (!response.ok) {
        return {
          status: 'error',
          message: `The app server returned ${response.status} ${response.statusText}.`,
        }
      }
      return { status: 'ready', ...((await response.json()) as FeedResponse) }
    }

    const refresh = (): void => {
      load()
        .then((next) => {
          if (!abort.signal.aborted) setState(next)
        })
        .catch((error: unknown) => {
          if (abort.signal.aborted) return
          setState({
            status: 'error',
            message: error instanceof Error ? error.message : String(error),
          })
        })
    }

    refresh()
    const timer = setInterval(refresh, REFRESH_INTERVAL_MS)

    return () => {
      clearInterval(timer)
      abort.abort()
    }
  }, [context])

  return state
}
