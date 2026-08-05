/**
 * Single source of truth for what the generator can produce. This alpha ships
 * exactly one module type, so there is nothing for the wizard to pick — widen
 * the constants here (and the template) as the platform grows more surfaces.
 */

/** The only module type the platform supports in this alpha. */
export const DASHBOARD_MODULE_KEY = 'project-dashboard-page' as const

/** Manifest `key` of the dashboard page every generated app contributes. */
export const DASHBOARD_ENTRY_KEY = 'main'

/** Route the dashboard iframe loads, relative to the manifest `baseUrl`. */
export const DASHBOARD_ENTRY_PATH = '/'

/**
 * Icon shown next to the app's item in the project menu: either an emoji or a
 * native Tolgee icon name. A name must match an exported icon component
 * exactly (`Key01`, not `Key`); anything else renders as literal text, which is
 * what makes the emoji work.
 */
export const DASHBOARD_ICON = '🧩'

/**
 * Scopes the generated manifest requests — enough for the scaffold's sample
 * REST call. The app is granted the intersection of these and the installing
 * user's project permissions, so widening them here is not a privilege grant.
 */
export const DEFAULT_SCOPES = ['keys.view', 'translations.view']

export type ConnectMode = 'manual' | 'auto'

export const CONNECT_MODES: {
  value: ConnectMode
  label: string
  hint: string
}[] = [
  {
    value: 'manual',
    label: 'Manual',
    hint: "Register the app yourself in Tolgee's UI (Organization → Apps)",
  },
  {
    value: 'auto',
    label: 'Self-register',
    hint: 'App self-registers on boot as a native (server-wide) app, using the registration secret',
  },
]

export const SDK_PACKAGE_NAME = '@tolgee/apps-sdk'

export type SdkMode = 'auto' | 'local' | 'published'

export const SDK_MODES: SdkMode[] = ['auto', 'local', 'published']

/**
 * Version the template is written against, used when the generator cannot see
 * the SDK sources. It is an exact version on purpose: the SDK's npm `latest` tag
 * points at an early alpha that predates most of the API, so any range (`*`,
 * `^…`) resolves to a package the generated app does not compile against.
 */
export const PUBLISHED_SDK_VERSION = '0.0.1-alpha.8'

/**
 * RELEASE MARKER — flip to `true` in the same commit that publishes
 * `PUBLISHED_SDK_VERSION` to npm, and keep the two in lockstep afterwards.
 *
 * While it is `false`, `resolveSdk` refuses to scaffold against the registry:
 * every version published so far predates `selfRegisterApp` and
 * `applyTolgeeTheme`, which the template imports, so the generated app would
 * fail to typecheck with a wall of missing-export errors instead of one
 * sentence naming the cause.
 */
export const PUBLISHED_SDK_RELEASED = false

export const DEFAULT_TOLGEE_URL = 'http://localhost:8718'
export const DEFAULT_VITE_PORT = 5180
export const DEFAULT_SERVER_PORT = 5181
