import {
  fetchAppInstallations,
  type AppEnabledProject,
  type TolgeeApiSchemas,
} from '@tolgee/apps-sdk/server'
import type { FeedResponse, ProjectFeed, WorkerStatus } from '../src/feedTypes'
import { config, workerConfig } from './config'
import { forgetTolgeeClient, tolgeeClient } from './tolgeeAccess'
import { translationChangesOf } from './translationChanges'

const feeds = new Map<number, ProjectFeed>()

let worker: WorkerStatus = {
  connected: false,
  installCount: 0,
  watchedProjectCount: 0,
  activityPollIntervalMs: workerConfig.activityPollIntervalMs,
  installationsRefreshIntervalMs: workerConfig.installationsRefreshIntervalMs,
  lastError: null,
}

export const feedFor = (projectId: number): FeedResponse => ({
  worker,
  feed: feeds.get(projectId) ?? null,
})

/**
 * Starts the two loops the app runs forever: one asking Tolgee what it is
 * installed for, one reading the activity of every project that answer named.
 *
 * Both are polls. The alpha has no way for Tolgee to push either an enablement
 * change or an activity event, so an app that wants to react to them has to ask.
 */
export const startActivityWorker = (): void => {
  void cycle()
  setInterval(() => {
    void refreshInstallations()
  }, workerConfig.installationsRefreshIntervalMs)
  setInterval(() => {
    void pollAll()
  }, workerConfig.activityPollIntervalMs)
}

const cycle = async (): Promise<void> => {
  await refreshInstallations()
  await pollAll()
}

/**
 * Re-reads which projects this app may act on. An organization admin can make
 * the app available and a project owner can enable it at any moment — and
 * either can undo it — so this list is never settled.
 */
const refreshInstallations = async (): Promise<void> => {
  try {
    const installations = await fetchAppInstallations({
      tolgeeUrl: config.tolgeeUrl,
    })
    const enabled = new Map<number, AppEnabledProject>()
    for (const installation of installations) {
      for (const project of installation.enabledProjects) {
        enabled.set(project.id, project)
      }
    }

    for (const projectId of [...feeds.keys()]) {
      if (!enabled.has(projectId)) feeds.delete(projectId)
    }
    for (const [projectId, project] of enabled) {
      if (!feeds.has(projectId)) feeds.set(projectId, emptyFeed(project))
    }

    worker = {
      ...worker,
      connected: true,
      installCount: installations.length,
      watchedProjectCount: enabled.size,
      lastError: null,
    }
  } catch (error) {
    worker = { ...worker, connected: false, lastError: messageOf(error) }
    console.error(
      `[worker] could not read this app's installations from ${config.tolgeeUrl}: ` +
        `${messageOf(error)}\n` +
        '  Has the app been registered, and is Tolgee running with tolgee.apps.enabled=true?'
    )
  }
}

const pollAll = async (): Promise<void> => {
  for (const feed of [...feeds.values()]) {
    await pollProject(feed)
  }
}

const pollProject = async (feed: ProjectFeed): Promise<void> => {
  try {
    const client = await tolgeeClient()
    const { data, response } = await client.GET(
      '/v2/projects/{projectId}/activity',
      {
        params: {
          path: { projectId: feed.projectId },
          query: { size: workerConfig.activityPageSize },
        },
      }
    )

    if (response.status === 401) forgetTolgeeClient()
    if (!data) {
      throw new Error(`Tolgee returned ${response.status} ${response.statusText}`)
    }

    ingest(feed, data._embedded?.activities ?? [])
    feed.lastPolledAt = new Date().toISOString()
    feed.lastError = null
  } catch (error) {
    feed.lastPolledAt = new Date().toISOString()
    feed.lastError = messageOf(error)
    console.error(
      `[worker] polling project ${feed.projectId} failed: ${messageOf(error)}`
    )
  }
}

/**
 * Folds a page of activity into the feed, newest first. Revisions already seen
 * are skipped: each poll re-reads the same newest page, so without this every
 * unchanged project would grow duplicates every interval.
 */
const ingest = (
  feed: ProjectFeed,
  revisions: TolgeeApiSchemas['ProjectActivityModel'][]
): void => {
  const known = new Set(feed.changes.map((change) => change.revisionId))
  const fresh = revisions
    .filter((revision) => !known.has(revision.revisionId))
    .flatMap((revision) => translationChangesOf(revision))

  if (fresh.length === 0) return

  feed.changes = [...fresh, ...feed.changes]
    .sort((a, b) => b.timestamp - a.timestamp)
    .slice(0, workerConfig.feedSize)
}

const emptyFeed = (project: AppEnabledProject): ProjectFeed => ({
  projectId: project.id,
  projectName: project.name,
  organizationSlug: project.organization.slug,
  changes: [],
  lastPolledAt: null,
  lastError: null,
})

const messageOf = (error: unknown): string =>
  error instanceof Error ? error.message : String(error)
