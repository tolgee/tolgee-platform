import { createApiClient } from '@tginternal/client'
import { normalizeTolgeeUrl } from '../shared/url'

/**
 * The typed REST client {@link createTolgeeAppServerClient} returns. Aliased
 * from `@tginternal/client` for the same reason as the browser one: the methods
 * are generated from Tolgee's OpenAPI schema, and app code should not have to
 * import `@tginternal/client` itself.
 */
export type TolgeeAppServerClient = ReturnType<typeof createApiClient>

export type TolgeeAppServerClientInput = {
  tolgeeUrl: string
  /** Install-context token, e.g. from `fetchAppAccessToken()`. */
  accessToken: string
}

/**
 * Builds a typed Tolgee REST client for an app's **backend**, authenticated
 * with an install-context access token. The browser counterpart is
 * `createTolgeeAppClient` in `@tolgee/apps-sdk/browser`, which takes its
 * credentials from the iframe context instead.
 *
 * Errors are returned in the `error` field rather than thrown.
 *
 *     const { accessToken } = await fetchAppAccessToken()
 *     const tolgee = createTolgeeAppServerClient({ tolgeeUrl, accessToken })
 *     const { data } = await tolgee.GET('/v2/projects/{projectId}/activity', {
 *       params: { path: { projectId }, query: { size: 20 } },
 *     })
 *
 * The token is short-lived, so build a fresh client when it expires rather than
 * holding one for the process lifetime.
 */
export const createTolgeeAppServerClient = (
  input: TolgeeAppServerClientInput
): TolgeeAppServerClient => {
  return createApiClient({
    baseUrl: normalizeTolgeeUrl(input.tolgeeUrl),
    userToken: input.accessToken,
    autoThrow: false,
  })
}
