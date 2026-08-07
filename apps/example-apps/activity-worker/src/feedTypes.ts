/** Shared between the worker that produces the feed and the page that shows it. */

/** One translation edit the worker saw in a project's activity. */
export type TranslationChange = {
  /** Activity revision the change came from; also the de-duplication key. */
  revisionId: number
  /** Unix millis, as Tolgee reports it. */
  timestamp: number
  authorName: string | null
  keyName: string | null
  namespace: string | null
  languageTag: string | null
  oldText: string | null
  newText: string | null
  /** Tolgee's activity type, e.g. `SET_TRANSLATIONS`. */
  activityType: string
}

export type ProjectFeed = {
  projectId: number
  projectName: string
  organizationSlug: string
  /** Newest first. */
  changes: TranslationChange[]
  /** ISO timestamp of the last completed poll; null before the first one. */
  lastPolledAt: string | null
  /** Why the last poll failed, or null when it succeeded. */
  lastError: string | null
}

export type WorkerStatus = {
  /** False until the worker has resolved which projects it may act on. */
  connected: boolean
  installCount: number
  watchedProjectCount: number
  activityPollIntervalMs: number
  installationsRefreshIntervalMs: number
  /** Why the app could not reach Tolgee, or null. */
  lastError: string | null
}

/** Response of `GET /api/feed`, for the project the caller's token is bound to. */
export type FeedResponse = {
  worker: WorkerStatus
  /** Null when the worker is not watching this project (yet). */
  feed: ProjectFeed | null
}
