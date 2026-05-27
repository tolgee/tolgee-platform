import { MODULES, type ModuleKey } from './registry'

export type ManifestInput = {
  id: string
  name: string
  selectedModules: ModuleKey[]
  webhookEvents: string[]
}

/**
 * Renders the contents of `server/manifest.template.json` from wizard
 * answers. The template uses `__BASE_URL__` placeholders that the
 * generated server substitutes at request time with the live tunnel
 * URL (or `http://localhost:5180` when no tunnel is active).
 */
export const buildManifest = (input: ManifestInput): string => {
  const modules: Record<string, unknown[]> = {}

  const tabKey = 'tab'
  const panelKey = 'panel'
  const modalKey = 'modal'

  for (const m of input.selectedModules) {
    modules[m] = entriesFor(m, { tabKey, panelKey, modalKey })
  }

  const manifest: Record<string, unknown> = {
    id: input.id,
    name: input.name,
    version: '0.1.0',
    baseUrl: '__BASE_URL__',
    decoratorsUrl: '__BASE_URL__/decorators',
    scopes: ['translations.view', 'keys.view'],
    modules,
  }

  if (input.webhookEvents.length > 0) {
    manifest.webhooks = {
      events: input.webhookEvents,
      url: '__BASE_URL__/webhook',
    }
  }

  return JSON.stringify(manifest, null, 2) + '\n'
}

const entriesFor = (
  key: ModuleKey,
  refs: { tabKey: string; panelKey: string; modalKey: string }
): unknown[] => {
  const info = MODULES.find((m) => m.key === key)
  if (!info) return []
  switch (key) {
    case 'project-dashboard-page':
      return [
        {
          key: info.defaultEntryKey,
          title: 'Dashboard',
          icon: 'LayoutAlt04',
          entry: info.entryPath,
        },
      ]
    case 'translation-tools-panel':
      return [
        {
          key: refs.panelKey,
          title: 'Plugin panel',
          icon: 'BarChart01',
          entry: info.entryPath,
        },
      ]
    case 'key-edit-tab':
      return [
        {
          key: refs.tabKey,
          title: 'Plugin tab',
          icon: 'Shield01',
          entry: info.entryPath,
        },
      ]
    case 'modal':
      return [
        {
          key: refs.modalKey,
          title: 'Plugin modal',
          icon: 'Lightbulb01',
          entry: info.entryPath,
          width: 640,
          height: 400,
        },
      ]
    case 'key-action':
      return [
        {
          key: info.defaultEntryKey,
          type: 'tab',
          icon: 'ArrowUpRight',
          tooltip: 'Open in plugin',
          tabKey: refs.tabKey,
        },
      ]
    case 'translation-action':
      return [
        {
          key: info.defaultEntryKey,
          type: 'panel',
          icon: 'ArrowUpRight',
          tooltip: 'Open plugin panel',
          panelKey: refs.panelKey,
        },
      ]
    case 'bulk-action':
      return [
        {
          key: info.defaultEntryKey,
          title: 'Run on selection',
          icon: 'Target01',
          type: 'modal',
          modalKey: refs.modalKey,
        },
      ]
    case 'translations-toolbar-action':
      return [
        {
          key: info.defaultEntryKey,
          title: 'Open plugin',
          icon: 'Download01',
          type: 'modal',
          modalKey: refs.modalKey,
        },
      ]
    case 'project-menu-action':
      return [
        {
          key: info.defaultEntryKey,
          title: 'Configure plugin',
          icon: 'Settings01',
          type: 'modal',
          modalKey: refs.modalKey,
        },
      ]
    case 'shortcut':
      return [
        {
          key: info.defaultEntryKey,
          combination: 'Mod+Shift+E',
          type: 'modal',
          modalKey: refs.modalKey,
        },
      ]
  }
}
