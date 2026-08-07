export type InvisibleCharKind = 'nonBreakingSpace' | 'zeroWidth';

export type InvisibleChar = {
  value: string;
  kind: InvisibleCharKind;
};

export const INVISIBLE_CHARACTERS: InvisibleChar[] = [
  { value: ' ', kind: 'nonBreakingSpace' },
  { value: ' ', kind: 'nonBreakingSpace' },
  { value: ' ', kind: 'nonBreakingSpace' },
  { value: '​', kind: 'zeroWidth' },
  { value: '﻿', kind: 'zeroWidth' },
  { value: '­', kind: 'zeroWidth' },
];

const BY_VALUE = new Map(
  INVISIBLE_CHARACTERS.map((char) => [char.value, char])
);

// Built from escapes rather than the raw characters so the regex source stays
// readable — literal invisible characters here would be the very problem this
// module exists to surface.
const CHARACTER_CLASS = `[${INVISIBLE_CHARACTERS.map(
  ({ value }) => `\\u${value.charCodeAt(0).toString(16).padStart(4, '0')}`
).join('')}]`;

const HAS_INVISIBLE = new RegExp(CHARACTER_CLASS);
const ALL_INVISIBLE = new RegExp(CHARACTER_CLASS, 'g');

export function findInvisibleCharacters(
  text: string
): { index: number; char: InvisibleChar }[] {
  if (!HAS_INVISIBLE.test(text)) {
    return [];
  }

  const found: { index: number; char: InvisibleChar }[] = [];
  ALL_INVISIBLE.lastIndex = 0;
  let match = ALL_INVISIBLE.exec(text);
  while (match !== null) {
    const char = BY_VALUE.get(match[0]);
    if (char) {
      found.push({ index: match.index, char });
    }
    match = ALL_INVISIBLE.exec(text);
  }
  return found;
}
