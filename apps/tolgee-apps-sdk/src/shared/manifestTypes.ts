import type { WebhookType } from './webhookTypes'

/**
 * Typed model of a Tolgee App manifest. This is the single source of truth
 * shared by `create-tolgee-app`'s builder, the generated `manifest.template.json`,
 * and hand-written apps. The JSON keys (incl. kebab-case module names) match
 * exactly what the platform's `AppManifestFetcher` parses and validates.
 */
export type AppManifest = {
  id: string
  name: string
  version: string
  baseUrl: string
  /** Endpoint the webapp POSTs to for dynamic row decorators. */
  decoratorsUrl?: string
  /** Tolgee permission scopes the app requests at install time (e.g. `keys.edit`). */
  scopes?: string[]
  webhooks?: AppWebhooks
  modules?: AppModules
}

export type AppWebhooks = {
  events: WebhookType[]
  /** Relative to `baseUrl` or absolute. */
  url: string
}

/**
 * The action surface kind. Trigger surfaces (bulk/toolbar/menu/shortcut) accept
 * only `link` and `modal`; `key-action` additionally accepts `tab`; `translation-action`
 * additionally accepts `panel`. The platform rejects invalid combinations at register time.
 */
export type AppActionType = 'link' | 'tab' | 'panel' | 'modal'

/** An iframe-bearing module (dashboard page, panel, key-edit tab, modal). */
export type AppIframeModule = {
  key: string
  title: string
  /** Named icon from the platform icon set (e.g. `LayoutAlt04`). */
  icon?: string
  /** Route the iframe loads, relative to `baseUrl`. */
  entry: string
  /** Modal-only: iframe pixel size. */
  width?: number
  height?: number
}

/** An action that triggers a link, an iframe module, or a modal. */
export type AppAction = {
  key: string
  type: AppActionType
  icon?: string
  tooltip?: string
  title?: string
  /** `link` actions: target URL, may contain placeholders like `{keyId}`. */
  urlTemplate?: string
  /** `tab` actions: which `key-edit-tab` to open. */
  tabKey?: string
  /** `panel` actions: which `translation-tools-panel` to open. */
  panelKey?: string
  /** `modal` actions: which `modal` to open. */
  modalKey?: string
  /**
   * When true, visibility/decoration of this action is driven by the app's
   * `decoratorsUrl` response rather than shown statically on every row.
   */
  dynamic?: boolean
}

/** A global keyboard-shortcut binding that triggers a link or modal. */
export type AppShortcut = Omit<AppAction, 'type'> & {
  /** e.g. `Mod+Shift+E`. */
  combination: string
  type: Extract<AppActionType, 'link' | 'modal'>
}

/**
 * App modules keyed by the platform's kebab-case module identifiers. Every
 * key is optional; an app contributes only the surfaces it needs.
 */
export type AppModules = {
  'project-dashboard-page'?: AppIframeModule[]
  'translation-tools-panel'?: AppIframeModule[]
  /** Panel shown in the translations tools area when no cell is being edited. */
  'translation-tools-panel-empty'?: AppIframeModule[]
  'key-edit-tab'?: AppIframeModule[]
  modal?: AppIframeModule[]
  'key-action'?: AppAction[]
  'translation-action'?: AppAction[]
  'bulk-action'?: AppAction[]
  'translations-toolbar-action'?: AppAction[]
  'project-menu-action'?: AppAction[]
  shortcut?: AppShortcut[]
}
