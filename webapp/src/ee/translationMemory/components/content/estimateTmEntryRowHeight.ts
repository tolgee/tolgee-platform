import {
  EntryRowLayout,
  TmRow,
} from 'tg.ee.module/translationMemory/components/content/TranslationMemoryEntryRow';

/**
 * Approximate row height for the virtualized ReactList. Real heights are measured on render
 * and override the estimate, so being slightly off only shows up as a tiny one-frame jump
 * before scroll position settles. Constants match the row paddings/line-heights in
 * TranslationMemoryEntryRow.styles.ts.
 */
export const estimateTmEntryRowHeight = (
  row: TmRow | undefined,
  displayLanguageCount: number,
  layout: EntryRowLayout
): number => {
  if (!row) return layout === 'flat' ? 64 : 120;
  // Source area: 12px padding top + 18px line + 12px padding bottom = 42px base, +20 for key.
  const keyLine = row.keyName ? 20 : 0;
  const sourceLineCount = Math.min(
    Math.max(1, Math.ceil(row.sourceText.length / 60)),
    3
  );
  const sourceArea = 24 + 18 * sourceLineCount + keyLine;
  if (layout === 'flat') {
    // Translation cells share the row height; max line count across them is unknown without
    // inspecting each entry, so assume 1 line which is true for most TM entries. Caller will
    // re-measure.
    return Math.max(48, sourceArea);
  }
  // Stacked mode renders one translation cell per displayed language under the source row.
  return sourceArea + Math.max(displayLanguageCount, 1) * 56;
};
