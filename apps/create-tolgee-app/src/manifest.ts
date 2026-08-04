import type { AppManifest, AppModules } from '@tolgee/apps-sdk'
import {
  DASHBOARD_ENTRY_KEY,
  DASHBOARD_ENTRY_PATH,
  DASHBOARD_ICON,
  DASHBOARD_MODULE_KEY,
  DEFAULT_SCOPES,
} from './registry'

export type ManifestInput = {
  id: string
  name: string
}

/**
 * Renders the contents of `server/manifest.template.json`. `baseUrl` stays a
 * `__BASE_URL__` placeholder: the generated server substitutes it on every
 * `/manifest.json` request, so the same file works whatever port or host the
 * app ends up on.
 */
export const buildManifest = (input: ManifestInput): string => {
  const modules: AppModules = {
    [DASHBOARD_MODULE_KEY]: [
      {
        key: DASHBOARD_ENTRY_KEY,
        title: input.name,
        icon: DASHBOARD_ICON,
        entry: DASHBOARD_ENTRY_PATH,
      },
    ],
  }

  const manifest: AppManifest = {
    id: input.id,
    name: input.name,
    version: '0.1.0',
    baseUrl: '__BASE_URL__',
    scopes: DEFAULT_SCOPES,
    modules,
  }

  return JSON.stringify(manifest, null, 2) + '\n'
}
