import type { IconMeaning } from './iconMeta';

export const OVERLAP_HINT = 'Overlaps with another icon';
export const UNUSED_HINT = 'Probably unused — verify before deleting';

/** The hint a grid tile carries, so hovering a glyph says what the list would. */
export const flagHint = (meaning?: IconMeaning) =>
  [
    meaning?.prefer && `${OVERLAP_HINT}: prefer ${meaning.prefer}`,
    meaning?.unused && UNUSED_HINT,
  ]
    .filter(Boolean)
    .join('\n');
