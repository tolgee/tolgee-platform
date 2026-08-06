import {
  createTolgeeAppServerClient,
  decodeContextToken,
} from '@tolgee/apps-sdk/server'
import type { Request } from 'express'
import { feedFor } from './activityWorker'
import { config } from './config'
import type { FeedResponse } from '../src/feedTypes'

export type FeedRouteResult =
  | { status: 200; body: FeedResponse }
  | { status: 401 | 403; body: { error: string } }

/**
 * Serves the in-memory feed of the project the caller's context token is bound
 * to.
 *
 * The token's claims decide nothing on their own — `decodeContextToken` does
 * not verify the signature, so anyone could forge a project id. Tolgee is asked
 * to validate the token instead, by replaying it against the very endpoint the
 * feed is built from: a token that cannot read a project's activity at Tolgee
 * cannot read this app's copy of it either.
 */
export const handleFeedRequest = async (
  request: Request
): Promise<FeedRouteResult> => {
  const token = bearerTokenOf(request.header('authorization'))
  if (!token) {
    return {
      status: 401,
      body: { error: 'Missing Tolgee app context token.' },
    }
  }

  const projectId = projectIdOf(token)
  if (projectId === null) {
    return {
      status: 401,
      body: { error: 'The token is not bound to a project.' },
    }
  }

  const client = createTolgeeAppServerClient({
    tolgeeUrl: config.tolgeeUrl,
    accessToken: token,
  })
  const { response } = await client.GET('/v2/projects/{projectId}/activity', {
    params: { path: { projectId }, query: { size: 1 } },
  })

  if (!response.ok) {
    return {
      status: response.status === 401 ? 401 : 403,
      body: {
        error: `Tolgee refused this token for project ${projectId} (${response.status}).`,
      },
    }
  }

  return { status: 200, body: feedFor(projectId) }
}

const bearerTokenOf = (header: string | undefined): string | null => {
  const match = /^Bearer (.+)$/i.exec(header ?? '')
  return match ? match[1] : null
}

const projectIdOf = (token: string): number | null => {
  try {
    return decodeContextToken(token).projectId
  } catch {
    return null
  }
}
