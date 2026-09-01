import { createApiClient } from '@tginternal/client'
import type { TolgeeAppContext } from '../shared/contextTypes'

/**
 * The typed REST client {@link createTolgeeAppClient} returns. Its methods are
 * generated from Tolgee's OpenAPI schema — that is the whole point of the
 * helper — so this is deliberately an alias of `@tginternal/client`'s client
 * rather than a hand-written interface, which could only be narrower and would
 * throw the path/response typing away. Name it through this alias so app code
 * never has to import `@tginternal/client` itself.
 */
export type TolgeeAppClient = ReturnType<typeof createApiClient>

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
): TolgeeAppClient => {
  return createApiClient({
    baseUrl: context.apiUrl,
    userToken: context.token,
    projectId: context.projectId,
    autoThrow: false,
  })
}
