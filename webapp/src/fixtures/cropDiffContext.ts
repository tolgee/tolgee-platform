/**
 * Crops a diff preview down to a context window around the affected
 * range so long translations don't fill the whole side panel.
 *
 * The `removed` segment is intentionally never cropped — the user must see
 * exactly what would be deleted. Only the surrounding context (`before` /
 * `after`) is windowed, and the `replacement` is bounded by a generous safety
 * cap to defend against pathological inputs.
 *
 * Cuts are grapheme-aligned (via `Intl.Segmenter` when available) so we don't
 * split surrogate pairs, ZWJ sequences, flag emojis, or combining accents.
 */

export const MAX_FULL_LENGTH = 120;
export const CONTEXT_BEFORE = 30;
export const CONTEXT_AFTER = 30;
export const WORD_BOUNDARY_TOLERANCE = 12;
export const MAX_REPLACEMENT_RENDER = 240;
const REPLACEMENT_HEAD_TAIL = 100;

export type CropDiffResult = {
  beforeEllipsis: boolean;
  before: string;
  removed: string;
  replacement: string;
  replacementEllipsised: boolean;
  after: string;
  afterEllipsis: boolean;
  isInsertion: boolean;
};

type GraphemeSplitter = (input: string) => string[];

/**
 * Splits `input` into graphemes. Uses `Intl.Segmenter` when available for true
 * grapheme-cluster awareness; otherwise falls back to a codepoint split, which
 * is correct for surrogate pairs but not for ZWJ sequences or combining marks.
 */
function makeSplitter(locale?: string): GraphemeSplitter {
  if (typeof Intl.Segmenter === 'function') {
    const segmenter = new Intl.Segmenter(locale, { granularity: 'grapheme' });
    return (input: string) => {
      const out: string[] = [];
      for (const seg of segmenter.segment(input)) {
        out.push(seg.segment);
      }
      return out;
    };
  }
  return (input: string) => Array.from(input);
}

/**
 * Returns the index (in grapheme array) of the nearest whitespace grapheme
 * within `tolerance` of `target`, searching outward from `target`.
 *
 * Returns -1 when no whitespace grapheme is found within tolerance.
 */
function findNearestWhitespace(
  graphemes: string[],
  target: number,
  tolerance: number
): number {
  for (let delta = 0; delta <= tolerance; delta++) {
    const left = target - delta;
    if (left >= 0 && left < graphemes.length && /^\s$/.test(graphemes[left])) {
      return left;
    }
    if (delta === 0) continue;
    const right = target + delta;
    if (
      right >= 0 &&
      right < graphemes.length &&
      /^\s$/.test(graphemes[right])
    ) {
      return right;
    }
  }
  return -1;
}

/**
 * Crops `before` from the right: keeps the trailing `keep` graphemes (snapped
 * to a whitespace boundary if one is within tolerance), trims trailing
 * whitespace from the kept slice, and reports whether an ellipsis is needed.
 */
function cropBefore(
  before: string,
  keep: number,
  splitter: GraphemeSplitter
): { text: string; ellipsis: boolean } {
  const graphemes = splitter(before);
  if (graphemes.length <= keep) {
    return { text: before, ellipsis: false };
  }
  const hardCutFromLeft = graphemes.length - keep;
  const ws = findNearestWhitespace(
    graphemes,
    hardCutFromLeft,
    WORD_BOUNDARY_TOLERANCE
  );
  // Cut just AFTER the whitespace so the kept slice starts at a word.
  let cut = ws >= 0 ? ws + 1 : hardCutFromLeft;
  // Skip a small run of further leading whitespace graphemes after the cut so
  // the ellipsis abuts visible content. Bounded so a long whitespace run
  // can't devour the kept slice.
  const maxSkip = WORD_BOUNDARY_TOLERANCE;
  let skipped = 0;
  while (
    skipped < maxSkip &&
    cut < graphemes.length &&
    /^\s$/.test(graphemes[cut])
  ) {
    cut++;
    skipped++;
  }
  return { text: graphemes.slice(cut).join(''), ellipsis: true };
}

/**
 * Crops `after` from the left: keeps the leading `keep` graphemes (snapped to
 * a whitespace boundary if one is within tolerance), trims leading whitespace
 * from the kept slice, and reports whether an ellipsis is needed.
 */
function cropAfter(
  after: string,
  keep: number,
  splitter: GraphemeSplitter
): { text: string; ellipsis: boolean } {
  const graphemes = splitter(after);
  if (graphemes.length <= keep) {
    return { text: after, ellipsis: false };
  }
  const ws = findNearestWhitespace(graphemes, keep, WORD_BOUNDARY_TOLERANCE);
  // Cut just BEFORE the whitespace so the kept slice ends at a word.
  let cut = ws >= 0 ? ws : keep;
  // Strip a small trailing whitespace run inside the kept slice so the
  // ellipsis abuts visible content. Bounded so a long whitespace run can't
  // devour the kept slice.
  const maxStrip = WORD_BOUNDARY_TOLERANCE;
  let stripped = 0;
  while (stripped < maxStrip && cut > 0 && /^\s$/.test(graphemes[cut - 1])) {
    cut--;
    stripped++;
  }
  return { text: graphemes.slice(0, cut).join(''), ellipsis: true };
}

/**
 * Caps an oversized replacement to a head + ellipsis + tail rendering.
 */
function capReplacement(
  replacement: string,
  splitter: GraphemeSplitter
): { text: string; ellipsised: boolean } {
  const graphemes = splitter(replacement);
  if (graphemes.length <= MAX_REPLACEMENT_RENDER) {
    return { text: replacement, ellipsised: false };
  }
  const head = graphemes.slice(0, REPLACEMENT_HEAD_TAIL).join('');
  const tail = graphemes.slice(-REPLACEMENT_HEAD_TAIL).join('');
  return { text: `${head}…${tail}`, ellipsised: true };
}

export function cropDiffContext(
  text: string,
  positionStart: number,
  positionEnd: number,
  replacement: string,
  locale?: string
): CropDiffResult {
  const before = text.slice(0, positionStart);
  const removed = text.slice(positionStart, positionEnd);
  const after = text.slice(positionEnd);
  const isInsertion = positionStart === positionEnd;

  const splitter = makeSplitter(locale);
  const cappedReplacement = capReplacement(replacement, splitter);

  if (text.length <= MAX_FULL_LENGTH) {
    return {
      beforeEllipsis: false,
      before,
      removed,
      replacement: cappedReplacement.text,
      replacementEllipsised: cappedReplacement.ellipsised,
      after,
      afterEllipsis: false,
      isInsertion,
    };
  }

  const beforeCrop = cropBefore(before, CONTEXT_BEFORE, splitter);
  const afterCrop = cropAfter(after, CONTEXT_AFTER, splitter);

  return {
    beforeEllipsis: beforeCrop.ellipsis,
    before: beforeCrop.text,
    removed,
    replacement: cappedReplacement.text,
    replacementEllipsised: cappedReplacement.ellipsised,
    after: afterCrop.text,
    afterEllipsis: afterCrop.ellipsis,
    isInsertion,
  };
}
