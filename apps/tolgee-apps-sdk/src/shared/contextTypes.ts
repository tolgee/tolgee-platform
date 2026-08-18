/**
 * The host's current theme. Delivered at init and again whenever the user
 * toggles light/dark, so the app can match Tolgee's look. `colors` are
 * resolved CSS color strings from Tolgee's palette; feed them to
 * `applyTolgeeTheme` to expose them as `--tg-color-*` CSS variables.
 */
export type TolgeeAppTheme = {
  mode: 'light' | 'dark'
  colors: {
    background: string
    backgroundPaper: string
    text: string
    textSecondary: string
    primary: string
    primaryContrast: string
    divider: string
    error: string
  }
}

/**
 * Context delivered to the iframe via the `tolgee-app:init` postMessage.
 * `token` is the install-context JWT — pass it as a bearer token on
 * every REST call back to Tolgee.
 */
export type TolgeeAppContext = {
  token: string
  apiUrl: string
  organizationId: number | null
  projectId: number
  /**
   * Revision of the app contract the host speaks. 1 in this alpha, and 1 for a
   * host that predates the field. Read it to keep working across a future
   * breaking revision rather than assuming the newest shape.
   */
  protocolVersion: number
  /** Host theme at init; subscribe to changes via `onThemeChanged`. */
  theme: TolgeeAppTheme
}

/**
 * Claims extracted from the Tolgee-issued JWT carried in
 * {@link TolgeeAppContext.token}. App backends generally don't have access to
 * the platform's signing key — they pass the same token as a bearer token on
 * REST calls back to Tolgee, which verifies it server-side.
 */
export type AppContextClaims = {
  installId: number
  /** Null for tokens not scoped to a single project. */
  projectId: number | null
  /** Null for tokens not issued on behalf of a user. */
  userId: number | null
  audience: string
  /** Unix seconds, as carried in the `exp` claim. */
  expiresAt: number
}
