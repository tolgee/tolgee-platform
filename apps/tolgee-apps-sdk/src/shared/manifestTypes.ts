/**
 * Typed model of a Tolgee App manifest. This is the single source of truth
 * shared by hand-written apps and the generated `manifest.template.json`. The
 * JSON keys (incl. the kebab-case module name) match exactly what the
 * platform's manifest fetcher parses and validates.
 *
 * This alpha supports a single module type: `project-dashboard-page`.
 */
export type AppManifest = {
  /** Stable app identifier, unique per Tolgee instance (e.g. `my-company.glossary`). */
  id: string
  name: string
  version: string
  /** Origin the app is served from; iframe entries resolve against it. */
  baseUrl: string
  /** Tolgee permission scopes the app requests at install time (e.g. `keys.edit`). */
  scopes?: string[]
  modules: AppModules
}

/**
 * App modules keyed by the platform's kebab-case module identifiers. The alpha
 * exposes only dashboard pages; more surfaces will be added as keys here.
 */
export type AppModules = {
  'project-dashboard-page'?: AppDashboardPage[]
}

/** A page rendered in an iframe under the project dashboard, with its own menu item. */
export type AppDashboardPage = {
  /** Unique within the app; part of the page's route in the webapp. */
  key: string
  /** Menu item label. */
  title: string
  /** Named icon from the platform icon set (e.g. `LayoutAlt04`). */
  icon: string
  /** Route the iframe loads, relative to `baseUrl`. */
  entry: string
}
