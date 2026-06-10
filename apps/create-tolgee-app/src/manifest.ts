import type {
  AppManifest,
  AppModules,
  WebhookType,
} from '@tolgee/apps-sdk'
import { MODULES, type ModuleKey } from './registry'

export type ManifestInput = {
  id: string
  name: string
  selectedModules: ModuleKey[]
  scopes: string[]
  webhookEvents: string[]
}

/**
 * Renders the contents of `server/manifest.template.json` from wizard
 * answers. The template uses `__BASE_URL__` placeholders that the
 * generated server substitutes at request time with the live tunnel
 * URL (or `http://localhost:5180` when no tunnel is active).
 */
export const buildManifest = (input: ManifestInput): string => {
  const modules: AppModules = {}

  for (const m of input.selectedModules) {
    modules[m] = entriesFor(m) as never
  }

  const manifest: AppManifest = {
    id: input.id,
    name: input.name,
    version: '0.1.0',
    baseUrl: '__BASE_URL__',
    decoratorsUrl: '__BASE_URL__/decorators',
    scopes: input.scopes,
    modules,
  }

  if (input.webhookEvents.length > 0) {
    manifest.webhooks = {
      events: input.webhookEvents as WebhookType[],
      url: '__BASE_URL__/webhook',
    }
  }

  return JSON.stringify(manifest, null, 2) + '\n'
}

const TAB_KEY = 'tab'
const PANEL_KEY = 'panel'
const MODAL_KEY = 'modal'

const entriesFor = (key: ModuleKey): unknown[] => {
  const info = MODULES.find((m) => m.key === key)
  if (!info) return []
  switch (key) {
    case 'project-dashboard-page':
      return [{ key: info.defaultEntryKey, title: 'Dashboard', icon: 'LayoutAlt04', entry: info.entryPath }]
    case 'translation-tools-panel':
      return [{ key: PANEL_KEY, title: 'Plugin panel', icon: 'BarChart01', entry: info.entryPath }]
    case 'translation-tools-panel-empty':
      return [{ key: 'empty-panel', title: 'Languages panel', icon: 'Globe01', entry: info.entryPath }]
    case 'key-edit-tab':
      return [{ key: TAB_KEY, title: 'Plugin tab', icon: 'Shield01', entry: info.entryPath }]
    case 'modal':
      return [
        { key: MODAL_KEY, title: 'Plugin modal', icon: 'Lightbulb01', entry: info.entryPath, width: 640, height: 400 },
      ]
    case 'key-action':
      // Two surfaces: a static one that opens a key-edit tab, and a dynamic
      // one whose icon/visibility is driven by the app's decoratorsUrl.
      return [
        { key: 'open-tab', type: 'tab', icon: 'ArrowUpRight', tooltip: 'Open in plugin', tabKey: TAB_KEY },
        { key: 'dynamic-flag', type: 'link', dynamic: true, icon: 'Lightbulb01', tooltip: 'Flagged by plugin' },
      ]
    case 'translation-action':
      return [{ key: info.defaultEntryKey, type: 'panel', icon: 'ArrowUpRight', tooltip: 'Open plugin panel', panelKey: PANEL_KEY }]
    case 'bulk-action':
      return [{ key: info.defaultEntryKey, title: 'Run on selection', icon: 'Target01', type: 'modal', modalKey: MODAL_KEY }]
    case 'translations-toolbar-action':
      // Static external link — opens the plugin's own page in a new tab.
      return [{ key: info.defaultEntryKey, title: 'Open plugin', icon: 'LinkExternal01', type: 'link', urlTemplate: '__BASE_URL__/' }]
    case 'project-menu-action':
      return [{ key: info.defaultEntryKey, title: 'Configure plugin', icon: 'Settings01', type: 'modal', modalKey: MODAL_KEY }]
    case 'shortcut':
      return [{ key: info.defaultEntryKey, combination: 'Mod+Shift+E', type: 'modal', modalKey: MODAL_KEY }]
  }
}
