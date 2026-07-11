import {
  hasOuterWhitespace,
  PO_MSGCTXT_KEY_SEPARATOR,
  splitKeyName,
} from '../keyName';

describe('splitKeyName', () => {
  it('returns only msgid for name without separator', () => {
    expect(splitKeyName('plain.key')).toEqual({ msgid: 'plain.key' });
  });

  it('splits name into msgctxt and msgid on first separator', () => {
    expect(splitKeyName(`menu${PO_MSGCTXT_KEY_SEPARATOR}Open`)).toEqual({
      msgctxt: 'menu',
      msgid: 'Open',
    });
  });

  it('treats only the first separator as boundary; later occurrences belong to msgid', () => {
    const name = `ctx${PO_MSGCTXT_KEY_SEPARATOR}id${PO_MSGCTXT_KEY_SEPARATOR}rest`;
    expect(splitKeyName(name)).toEqual({
      msgctxt: 'ctx',
      msgid: `id${PO_MSGCTXT_KEY_SEPARATOR}rest`,
    });
  });

  it('returns empty msgctxt when separator is at start', () => {
    expect(splitKeyName(`${PO_MSGCTXT_KEY_SEPARATOR}Cancel`)).toEqual({
      msgctxt: '',
      msgid: 'Cancel',
    });
  });

  it('returns empty msgid for empty input', () => {
    expect(splitKeyName('')).toEqual({ msgid: '' });
  });
});

describe('hasOuterWhitespace', () => {
  it.each(['key', '', 'inner key', 'a.b.c'])(
    'returns false for clean name %p',
    (name) => {
      expect(hasOuterWhitespace(name)).toBe(false);
    }
  );

  it.each([' key', 'key ', '  key  ', '\tkey', 'key\t', '\tkey\t'])(
    'returns true for name with outer whitespace %p',
    (name) => {
      expect(hasOuterWhitespace(name)).toBe(true);
    }
  );
});
