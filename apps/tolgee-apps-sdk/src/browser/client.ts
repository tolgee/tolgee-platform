import { createApiClient } from '@tginternal/client'
import type { TolgeeAppContext } from '../shared/contextTypes'

/**
 * Builds a typed Tolgee REST client wired with the install-context
 * token, base URL, and project id from the iframe's
 * `TolgeeAppContext`. Errors are returned in the `error` field rather
 * than thrown, matching `createApiClient`'s `autoThrow: false` mode.
 *
 *     const app = createTolgeeApp()
 *     const ctx = await app.context
 *     const tolgee = createTolgeeAppClient(ctx)
 *     const { data, error } = await tolgee.GET('/v2/projects/{projectId}', {
 *       params: { path: { projectId: ctx.projectId } },
 *     })
 */
export const createTolgeeAppClient = (
  context: TolgeeAppContext
): ReturnType<typeof createApiClient> => {
  return createApiClient({
    baseUrl: context.apiUrl,
    userToken: context.token,
    projectId: context.projectId,
    autoThrow: false,
  })
}
