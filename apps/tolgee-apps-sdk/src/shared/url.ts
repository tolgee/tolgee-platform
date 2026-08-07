/**
 * Canonical form of a Tolgee base URL: credentials are stored under it as a key
 * *and* request URLs are built from it, so both have to agree that
 * `http://localhost:8718/` and ` http://localhost:8718` name one instance.
 */
export const normalizeTolgeeUrl = (tolgeeUrl: string): string =>
  tolgeeUrl.trim().replace(/\/+$/, '')
