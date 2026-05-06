import {
  cropDiffContext,
  CONTEXT_AFTER,
  CONTEXT_BEFORE,
  MAX_FULL_LENGTH,
  MAX_REPLACEMENT_RENDER,
} from '../cropDiffContext';

describe('cropDiffContext', () => {
  describe('short text', () => {
    it('does not truncate text below threshold', () => {
      const text = 'Hello world!';
      const result = cropDiffContext(text, 6, 11, 'earth');
      expect(result.beforeEllipsis).toBe(false);
      expect(result.afterEllipsis).toBe(false);
      expect(result.before).toBe('Hello ');
      expect(result.removed).toBe('world');
      expect(result.replacement).toBe('earth');
      expect(result.after).toBe('!');
      expect(result.isInsertion).toBe(false);
    });

    it('exactly MAX_FULL_LENGTH is not cropped', () => {
      const text = 'a'.repeat(MAX_FULL_LENGTH - 3) + 'BAD';
      expect(text.length).toBe(MAX_FULL_LENGTH);
      const result = cropDiffContext(
        text,
        MAX_FULL_LENGTH - 3,
        MAX_FULL_LENGTH,
        'GOOD'
      );
      expect(result.beforeEllipsis).toBe(false);
      expect(result.afterEllipsis).toBe(false);
    });

    it('MAX_FULL_LENGTH + 1 is the first cropped length', () => {
      const text = 'a'.repeat(MAX_FULL_LENGTH - 2) + 'BAD';
      expect(text.length).toBe(MAX_FULL_LENGTH + 1);
      const result = cropDiffContext(
        text,
        MAX_FULL_LENGTH - 2,
        MAX_FULL_LENGTH + 1,
        'GOOD'
      );
      expect(result.beforeEllipsis).toBe(true);
      expect(result.afterEllipsis).toBe(false);
    });
  });

  describe('long text', () => {
    const longText =
      'A'.repeat(80) + ' ' + 'foo BAD bar' + ' ' + 'B'.repeat(80);
    // positions of "BAD" — word "BAD" starts at offset 80 + 1 + 4 = 85, ends at 88
    const start = 85;
    const end = 88;

    it('crops on both sides when removal is in the middle of a long text', () => {
      const result = cropDiffContext(longText, start, end, 'GOOD');
      expect(result.beforeEllipsis).toBe(true);
      expect(result.afterEllipsis).toBe(true);
      expect(result.before.length).toBeLessThanOrEqual(
        CONTEXT_BEFORE + 4 /* word-boundary slack */
      );
      expect(result.after.length).toBeLessThanOrEqual(CONTEXT_AFTER + 4);
      expect(result.removed).toBe('BAD');
      expect(result.replacement).toBe('GOOD');
    });

    it('omits leading ellipsis when change is near the start', () => {
      const text = 'BAD ' + 'x'.repeat(MAX_FULL_LENGTH);
      const result = cropDiffContext(text, 0, 3, 'GOOD');
      expect(result.beforeEllipsis).toBe(false);
      expect(result.afterEllipsis).toBe(true);
      expect(result.before).toBe('');
    });

    it('omits trailing ellipsis when change is near the end', () => {
      const text = 'x'.repeat(MAX_FULL_LENGTH) + ' BAD';
      const start = MAX_FULL_LENGTH + 1;
      const end = MAX_FULL_LENGTH + 4;
      const result = cropDiffContext(text, start, end, 'GOOD');
      expect(result.beforeEllipsis).toBe(true);
      expect(result.afterEllipsis).toBe(false);
      expect(result.after).toBe('');
    });

    it('asymmetric: text just over threshold with change near the end', () => {
      const text = 'x'.repeat(MAX_FULL_LENGTH - 1) + ' BAD';
      // length === MAX_FULL_LENGTH + 3
      const start = MAX_FULL_LENGTH;
      const end = MAX_FULL_LENGTH + 3;
      const result = cropDiffContext(text, start, end, 'GOOD');
      expect(text.length).toBe(MAX_FULL_LENGTH + 3);
      expect(result.beforeEllipsis).toBe(true);
      expect(result.afterEllipsis).toBe(false);
    });
  });

  describe('insertion', () => {
    it('marks isInsertion when positionStart === positionEnd', () => {
      const result = cropDiffContext('hello world', 5, 5, ' there');
      expect(result.isInsertion).toBe(true);
      expect(result.removed).toBe('');
    });

    it('insertion at very start', () => {
      const result = cropDiffContext('hello world', 0, 0, '> ');
      expect(result.isInsertion).toBe(true);
      expect(result.before).toBe('');
    });

    it('insertion at very end', () => {
      const result = cropDiffContext('hello world', 11, 11, '!');
      expect(result.isInsertion).toBe(true);
      expect(result.after).toBe('');
    });
  });

  describe('removed segment', () => {
    it('renders a long removed segment in full (never cropped)', () => {
      const removedSegment = 'X'.repeat(300);
      const text =
        'before context here ' + removedSegment + ' after context here';
      const start = 'before context here '.length;
      const end = start + removedSegment.length;
      const result = cropDiffContext(text, start, end, '');
      expect(result.removed).toBe(removedSegment);
      expect(result.removed.length).toBe(300);
    });
  });

  describe('word-boundary preference', () => {
    it('prefers cutting at a whitespace within tolerance', () => {
      // 'before' has 80 chars total ending in "...lazy dog" type words.
      const before = 'a'.repeat(60) + ' jumped over the lazy quick brown fox ';
      const text = before + 'BAD' + 'tail';
      const start = before.length;
      const end = start + 3;
      const result = cropDiffContext(text, start, end, 'GOOD');
      // The kept slice should start right after a whitespace, so it should
      // not start with the middle of a word.
      expect(result.before.length).toBeGreaterThan(0);
      expect(result.before.startsWith(' ')).toBe(false);
      // Verify we kept content and a real word starts the slice.
      expect(result.before).toMatch(/^[A-Za-z]/);
    });

    it('falls back to hard cut when no whitespace within tolerance', () => {
      const before = 'a'.repeat(200); // no whitespace
      const text = before + 'BAD' + 'b'.repeat(50);
      const start = before.length;
      const end = start + 3;
      const result = cropDiffContext(text, start, end, 'GOOD');
      expect(result.beforeEllipsis).toBe(true);
      expect(result.before.length).toBe(CONTEXT_BEFORE);
    });

    it('trims leading whitespace from cropped before so the ellipsis abuts visible content', () => {
      // 80 chars + "     word ..." — the cut for keep=30 lands inside 'a'-run,
      // skips no whitespace; force whitespace at the cut by padding so the
      // grapheme right after the cut is whitespace.
      const before = 'a'.repeat(120) + '     foo bar baz qux';
      const text = before + 'BAD' + 'tail';
      const start = before.length;
      const end = start + 3;
      const result = cropDiffContext(text, start, end, 'GOOD');
      expect(result.beforeEllipsis).toBe(true);
      expect(result.before.startsWith(' ')).toBe(false);
    });

    it('trims trailing whitespace from cropped after so the ellipsis abuts visible content', () => {
      const after = 'foo bar baz qux     ' + 'b'.repeat(120);
      const text = 'head' + 'BAD' + after;
      const start = 'head'.length;
      const end = start + 3;
      const result = cropDiffContext(text, start, end, 'GOOD');
      expect(result.afterEllipsis).toBe(true);
      expect(result.after.endsWith(' ')).toBe(false);
    });
  });

  describe('grapheme clusters', () => {
    it('does not split a surrogate pair at the cut boundary', () => {
      // 😀 = "😀", 2 UTF-16 code units, 1 grapheme.
      // Place an emoji exactly at the would-be cut so a code-unit split would
      // produce a lone high surrogate.
      const padding = 'a'.repeat(100);
      const before = padding + '😀😀😀😀😀'; // 5 emojis = 10 code units
      const text = before + 'BAD' + 'tail';
      const start = before.length;
      const end = start + 3;
      const result = cropDiffContext(text, start, end, 'GOOD');
      // Verify no lone surrogate
      for (let i = 0; i < result.before.length; i++) {
        const code = result.before.charCodeAt(i);
        if (code >= 0xd800 && code <= 0xdbff) {
          // high surrogate — must be followed by low surrogate
          const next = result.before.charCodeAt(i + 1);
          expect(next).toBeGreaterThanOrEqual(0xdc00);
          expect(next).toBeLessThanOrEqual(0xdfff);
        }
      }
    });

    it('does not split a ZWJ family emoji at the cut boundary', () => {
      // 👨‍👩‍👧 = man + ZWJ + woman + ZWJ + girl (1 grapheme)
      const family = '\u{1F468}‍\u{1F469}‍\u{1F467}';
      const before = 'a'.repeat(80) + family + family + family;
      const text = before + 'BAD' + 'tail';
      const start = before.length;
      const end = start + 3;
      const result = cropDiffContext(text, start, end, 'GOOD');
      // The kept `before` should not start or end mid-cluster (no lone ZWJ).
      expect(result.before.startsWith('‍')).toBe(false);
      expect(result.before.endsWith('‍')).toBe(false);
    });

    it('does not split a combining accent at the cut boundary', () => {
      // "é" as e + COMBINING ACUTE (U+0301)
      const accented = 'é'.repeat(40);
      const before = 'a'.repeat(60) + accented;
      const text = before + 'BAD' + 'tail';
      const start = before.length;
      const end = start + 3;
      const result = cropDiffContext(text, start, end, 'GOOD');
      // The kept `before` must not start with a lone combining mark.
      expect(result.before.startsWith('́')).toBe(false);
    });

    it('whitespace search runs in grapheme space and does not land inside a ZWJ cluster', () => {
      const family = '\u{1F468}‍\u{1F469}‍\u{1F467}';
      // Build a `before` whose hard-cut grapheme position falls inside a
      // family cluster's region; ensure no surrogate/ZWJ leak.
      const before = 'word ' + family.repeat(20) + ' tail';
      const text = before + 'BAD';
      const start = before.length;
      const end = start + 3;
      const result = cropDiffContext(text, start, end, 'GOOD');
      // Sanity: kept slice doesn't begin or end with a ZWJ.
      expect(result.before.startsWith('‍')).toBe(false);
      expect(result.before.endsWith('‍')).toBe(false);
    });
  });

  describe('replacement bounds', () => {
    it('passes through normal replacements unchanged', () => {
      const result = cropDiffContext('abcde', 1, 4, 'XYZ');
      expect(result.replacement).toBe('XYZ');
      expect(result.replacementEllipsised).toBe(false);
    });

    it('inner-ellipsises replacements over MAX_REPLACEMENT_RENDER', () => {
      const huge = 'x'.repeat(MAX_REPLACEMENT_RENDER + 100);
      const result = cropDiffContext('abcde', 1, 4, huge);
      expect(result.replacementEllipsised).toBe(true);
      expect(result.replacement).toContain('…');
      expect(result.replacement.length).toBeLessThan(huge.length);
    });
  });

  describe('RTL', () => {
    it('crops Arabic text correctly (string-level, not BIDI render)', () => {
      // "السلام عليكم ورحمة الله وبركاته" repeated to exceed threshold
      const arabic = 'السلام عليكم ورحمة الله وبركاته ';
      const text = arabic.repeat(10) + 'BAD' + arabic.repeat(10);
      const start = arabic.length * 10;
      const end = start + 3;
      const result = cropDiffContext(text, start, end, 'GOOD');
      expect(result.beforeEllipsis).toBe(true);
      expect(result.afterEllipsis).toBe(true);
      expect(result.removed).toBe('BAD');
      // No assertion on visual order — handled by <bdi> at render time.
    });
  });
});
