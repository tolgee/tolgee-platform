/**
 * Single source of truth for the modules and webhook events the wizard
 * offers. Add to these to expand what the generator can scaffold.
 */

export type ModuleKey =
  | 'project-dashboard-page'
  | 'translation-tools-panel'
  | 'translation-tools-panel-empty'
  | 'key-edit-tab'
  | 'modal'
  | 'key-action'
  | 'translation-action'
  | 'bulk-action'
  | 'translations-toolbar-action'
  | 'project-menu-action'
  | 'shortcut'

export type ModuleInfo = {
  key: ModuleKey
  label: string
  description: string
  /**
   * Iframe-bearing modules need a source-file stub under
   * `template/modules/<key>/`. Other module types only get a manifest
   * entry that points at another iframe module or a URL template.
   */
  hasIframe: boolean
  /** Stable slug used inside the manifest's `key` field. */
  defaultEntryKey: string
  /** Path the manifest's `entry` (or `urlTemplate`) field uses. */
  entryPath?: string
  /** Required only for `key-action` / `translation-action`. */
  needsTabOrPanelKey?: 'tab' | 'panel'
}

export const MODULES: ModuleInfo[] = [
  {
    key: 'project-dashboard-page',
    label: 'Project dashboard page',
    description: 'Full-area page in the project sidebar.',
    hasIframe: true,
    defaultEntryKey: 'dashboard',
    entryPath: '/dashboard',
  },
  {
    key: 'translation-tools-panel',
    label: 'Translation tools panel',
    description: 'Collapsible right-side panel in the translations view.',
    hasIframe: true,
    defaultEntryKey: 'panel',
    entryPath: '/tools-panel',
  },
  {
    key: 'translation-tools-panel-empty',
    label: 'Translation tools panel (no selection)',
    description:
      'Panel shown in the translations tools area when no cell is selected; receives the selected language tags.',
    hasIframe: true,
    defaultEntryKey: 'empty-panel',
    entryPath: '/tools-panel-empty',
  },
  {
    key: 'key-edit-tab',
    label: 'Key-edit dialog tab',
    description: 'Tab inside the key-edit modal.',
    hasIframe: true,
    defaultEntryKey: 'tab',
    entryPath: '/key-edit-tab',
  },
  {
    key: 'modal',
    label: 'Modal',
    description: 'Iframe modal triggered from any *-action surface.',
    hasIframe: true,
    defaultEntryKey: 'modal',
    entryPath: '/modal',
  },
  {
    key: 'key-action',
    label: 'Key action icon',
    description:
      'Icon button on each key row that opens a modal, key-edit tab, or external link.',
    hasIframe: false,
    defaultEntryKey: 'key-shortcut',
    needsTabOrPanelKey: 'tab',
  },
  {
    key: 'translation-action',
    label: 'Translation action icon',
    description:
      'Icon button on each translation cell that opens a panel or external link.',
    hasIframe: false,
    defaultEntryKey: 'translation-shortcut',
    needsTabOrPanelKey: 'panel',
  },
  {
    key: 'bulk-action',
    label: 'Bulk action',
    description: 'Item in the bulk-action bar when ≥1 keys are selected.',
    hasIframe: false,
    defaultEntryKey: 'bulk-explain',
  },
  {
    key: 'translations-toolbar-action',
    label: 'Translations toolbar action',
    description: 'Button in the translations-view toolbar.',
    hasIframe: false,
    defaultEntryKey: 'toolbar-explain',
  },
  {
    key: 'project-menu-action',
    label: 'Project menu action',
    description: 'Sidebar entry that opens a modal instead of routing.',
    hasIframe: false,
    defaultEntryKey: 'menu-explain',
  },
  {
    key: 'shortcut',
    label: 'Keyboard shortcut',
    description: 'Global keyboard shortcut binding (e.g. Mod+Shift+E).',
    hasIframe: false,
    defaultEntryKey: 'shortcut-explain',
  },
]

/**
 * Curated subset of Tolgee permission scopes the wizard offers. Each value is a
 * valid `Scope` the platform accepts in `manifest.scopes`; the app is granted the
 * intersection of these and the installing user's project permissions at runtime.
 */
export const SCOPES: { value: string; label: string }[] = [
  { value: 'translations.view', label: 'Read translations' },
  { value: 'translations.edit', label: 'Edit translations' },
  { value: 'translations.state-edit', label: 'Change translation states' },
  { value: 'translations.suggest', label: 'Suggest translations' },
  { value: 'keys.view', label: 'Read keys' },
  { value: 'keys.create', label: 'Create keys' },
  { value: 'keys.edit', label: 'Edit keys' },
  { value: 'keys.delete', label: 'Delete keys' },
  { value: 'screenshots.view', label: 'View screenshots' },
  { value: 'screenshots.upload', label: 'Upload screenshots' },
  { value: 'activity.view', label: 'View activity' },
]

/**
 * Curated subset of Tolgee `ActivityType` values most apps care about.
 * The full list is large; the wizard surfaces these by default with an
 * "I'll edit manifest.template.json myself" escape hatch.
 */
export const WEBHOOK_EVENTS: { value: string; label: string }[] = [
  { value: 'SET_TRANSLATIONS', label: 'Translation text changed' },
  { value: 'SET_TRANSLATION_STATE', label: 'Translation state changed' },
  { value: 'CREATE_KEY', label: 'Key created' },
  { value: 'KEY_DELETE', label: 'Key deleted' },
  { value: 'KEY_NAME_EDIT', label: 'Key renamed' },
  { value: 'KEY_TAGS_EDIT', label: 'Key tags changed' },
  {
    value: 'BATCH_SET_TRANSLATION_STATE',
    label: 'Translation state changed (batch)',
  },
  { value: 'BATCH_KEY_DELETE', label: 'Keys deleted (batch)' },
  { value: 'BATCH_KEY_RESTORE', label: 'Keys restored (batch)' },
  { value: 'IMPORT', label: 'Import finished' },
]
