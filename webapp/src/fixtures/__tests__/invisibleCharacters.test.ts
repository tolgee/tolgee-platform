import {
  findInvisibleCharacters,
  INVISIBLE_CHARACTERS,
} from '../invisibleCharacters';

describe('INVISIBLE_CHARACTERS', () => {
  it('registers each character exactly once', () => {
    const values = INVISIBLE_CHARACTERS.map(({ value }) => value);
    expect(new Set(values).size).toBe(values.length);
  });

  it('registers only single code units', () => {
    INVISIBLE_CHARACTERS.forEach(({ value }) => {
      expect(value).toHaveLength(1);
    });
  });
});

describe('findInvisibleCharacters', () => {
  it('returns nothing for an empty string', () => {
    expect(findInvisibleCharacters('')).toEqual([]);
  });

  it('returns nothing for text with no invisible characters', () => {
    expect(findInvisibleCharacters('a plain sentence')).toEqual([]);
  });

  it('does not match a regular space', () => {
    expect(findInvisibleCharacters('a b c')).toEqual([]);
  });

  it.each([
    ['\u00A0', 'nonBreakingSpace'],
    ['\u202F', 'nonBreakingSpace'],
    ['\u2007', 'nonBreakingSpace'],
    ['\u200B', 'zeroWidth'],
    ['\uFEFF', 'zeroWidth'],
    ['\u00AD', 'zeroWidth'],
  ])('finds %j and reports kind %s', (value, kind) => {
    expect(findInvisibleCharacters(`a${value}b`)).toEqual([
      { index: 1, char: { value, kind } },
    ]);
  });

  it('finds a character at the first index', () => {
    expect(findInvisibleCharacters('\u00A0abc')).toEqual([
      { index: 0, char: { value: '\u00A0', kind: 'nonBreakingSpace' } },
    ]);
  });

  it('finds a character at the last index', () => {
    expect(findInvisibleCharacters('abc\u00A0')).toEqual([
      { index: 3, char: { value: '\u00A0', kind: 'nonBreakingSpace' } },
    ]);
  });

  it('finds multiple characters in ascending order', () => {
    expect(findInvisibleCharacters('a\u00A0b\u200Bc')).toEqual([
      { index: 1, char: { value: '\u00A0', kind: 'nonBreakingSpace' } },
      { index: 3, char: { value: '\u200B', kind: 'zeroWidth' } },
    ]);
  });

  it('finds adjacent characters', () => {
    expect(findInvisibleCharacters('\u00A0\u00A0')).toEqual([
      { index: 0, char: { value: '\u00A0', kind: 'nonBreakingSpace' } },
      { index: 1, char: { value: '\u00A0', kind: 'nonBreakingSpace' } },
    ]);
  });

  it('reports code-unit offsets in text containing surrogate pairs', () => {
    expect(findInvisibleCharacters('\u{1f600}\u00A0')).toEqual([
      { index: 2, char: { value: '\u00A0', kind: 'nonBreakingSpace' } },
    ]);
  });

  it('is stable across repeated calls', () => {
    const text = 'a\u00A0b';
    expect(findInvisibleCharacters(text)).toEqual(
      findInvisibleCharacters(text)
    );
  });
});
