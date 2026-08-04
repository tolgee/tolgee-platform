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
  /** Null when the install is not owned by an organization. */
  organizationId: number | null
  projectId: number
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
