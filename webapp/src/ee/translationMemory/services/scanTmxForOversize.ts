/**
 * Mirror of `TranslationMemoryEntry.MAX_TEXT_LENGTH` on the backend
 * (`backend/data/.../model/translationMemory/TranslationMemoryEntry.kt`).
 * Keep in sync — both sides cap source/target text at the same length.
 */
export const TM_ENTRY_TEXT_MAX_LENGTH = 10_000;

/**
 * Files larger than this are NOT scanned client-side — reading + regex over a multi-MB string
 * blocks the main thread for too long on memory-constrained devices. Backend still enforces the
 * cap and reports any drops via `TmxImportResult.skipped`.
 *
 * 10 MB covers ~95% of real TMX uploads. The backend's default upload cap is 50 MB, so this
 * scan boundary lets us cover the typical case without risking tab freezes for the long tail.
 */
export const TMX_SCAN_MAX_FILE_BYTES = 10 * 1024 * 1024;

/**
 * Counts `<seg>...</seg>` blocks whose inner content exceeds [maxLen] characters.
 *
 * The regex is intentionally loose — inline markup like `<bpt>`, `<ph>` inside a segment gets
 * counted against the length. That biases toward over-warning, which is harmless: a false
 * positive at the boundary means the user is told a segment is "long" when in fact only its
 * inline tags pushed it over. The alternative (a real XML parse) is far too expensive client-side.
 */
export function scanTmxForOversize(
  text: string,
  maxLen: number
): { oversizeSegments: number } {
  const re = /<seg(?:\s[^>]*)?>([\s\S]*?)<\/seg>/g;
  let count = 0;
  let m: RegExpExecArray | null;
  while ((m = re.exec(text)) !== null) {
    if (m[1].length > maxLen) {
      count++;
    }
  }
  return { oversizeSegments: count };
}
