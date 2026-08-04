export type SelfRegisterInput = {
  /** Base URL of the Tolgee instance to register with. */
  tolgeeUrl: string
  /** Instance-wide secret Tolgee requires to accept a self-registration. */
  registrationSecret: string
  /** Organization the app is installed into. */
  organizationSlug: string
  /** Publicly reachable URL Tolgee fetches the manifest from. */
  manifestUrl: string
}

export type SelfRegisterResult = {
  installId: number
  clientId: string | null
  /**
   * The one-time client secret. Non-null only when this call created the
   * install — persist it immediately, Tolgee will not return it again.
   */
  clientSecret: string | null
  /** False when an existing install was repointed at `manifestUrl` instead. */
  created: boolean
}

/**
 * Registers (or repoints) this app on a Tolgee instance without anyone
 * clicking through the UI — the flow a dev app uses on startup, when its
 * tunnel URL changes on every restart.
 *
 * Tolgee returns the client secret only when it creates the install; calling
 * again for an already-registered app returns it as null and leaves the
 * existing credentials valid, which is what `created` reflects.
 *
 *     const { installId, clientId, clientSecret, created } = await selfRegisterApp({
 *       tolgeeUrl: config.tolgeeUrl,
 *       registrationSecret: config.registrationSecret!,
 *       organizationSlug: config.organizationSlug!,
 *       manifestUrl: `${baseUrl}/manifest.json`,
 *     })
 */
export const selfRegisterApp = async (
  input: SelfRegisterInput
): Promise<SelfRegisterResult> => {
  const url = `${trimTrailingSlash(input.tolgeeUrl)}/v2/public/apps/self-register`
  const response = await fetch(url, {
    method: 'POST',
    headers: {
      'Content-Type': 'application/json',
      'X-Tolgee-App-Registration-Secret': input.registrationSecret,
    },
    body: JSON.stringify({
      manifestUrl: input.manifestUrl,
      organizationSlug: input.organizationSlug,
    }),
  })

  if (!response.ok) {
    throw new Error(
      `Tolgee app self-registration failed: ${response.status} ${response.statusText} — ${await response.text()}`
    )
  }

  const body = (await response.json()) as {
    id?: unknown
    clientId?: unknown
    clientSecret?: unknown
  }
  if (typeof body.id !== 'number') {
    throw new Error(
      `Tolgee app self-registration returned no install id: ${JSON.stringify(body)}`
    )
  }
  const clientSecret =
    typeof body.clientSecret === 'string' ? body.clientSecret : null
  return {
    installId: body.id,
    clientId: typeof body.clientId === 'string' ? body.clientId : null,
    clientSecret,
    created: clientSecret !== null,
  }
}

const trimTrailingSlash = (url: string): string => url.replace(/\/+$/, '')
