const BASE_URL_PLACEHOLDER = '__BASE_URL__'

/**
 * Substitutes the `__BASE_URL__` placeholder in a manifest template with the
 * app's currently-reachable base URL (a tunnel URL in dev, the deployed origin
 * in production). Keeping the placeholder in the stored template lets the URL
 * change between dev restarts without editing the manifest.
 */
export const renderManifest = (template: string, baseUrl: string): string =>
  template.replaceAll(BASE_URL_PLACEHOLDER, baseUrl)
