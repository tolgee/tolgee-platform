import { parseSearchQuery } from '../parseSearchQuery';

const LANGS = ['en', 'de', 'en-US'];

describe('parseSearchQuery', () => {
  it('parses plain text as text tokens', () => {
    const result = parseSearchQuery('hello world', LANGS);
    expect(result.hasRecognizedQualifiers).toBe(false);
    expect(result.tokens).toEqual([
      { type: 'text', raw: 'hello', value: 'hello', from: 0, to: 5 },
      { type: 'text', raw: 'world', value: 'world', from: 6, to: 11 },
    ]);
  });

  it.each(['key', 'description', 'namespace', 'translation'])(
    'parses %s qualifier',
    (qualifier) => {
      const result = parseSearchQuery(`${qualifier}:foo`, LANGS);
      expect(result.tokens[0]).toMatchObject({
        type: 'scoped',
        qualifier,
        negated: false,
        value: 'foo',
      });
    }
  );

  it('parses qualifier case-insensitively', () => {
    const result = parseSearchQuery('KEY:foo', LANGS);
    expect(result.tokens[0]).toMatchObject({ qualifier: 'key', value: 'foo' });
  });

  it('parses negated qualifier', () => {
    const result = parseSearchQuery('-description:legacy', LANGS);
    expect(result.tokens[0]).toMatchObject({
      type: 'scoped',
      qualifier: 'description',
      negated: true,
      value: 'legacy',
    });
  });

  it('parses language tag qualifier with canonical tag', () => {
    const result = parseSearchQuery('en-us:cart', LANGS);
    expect(result.tokens[0]).toMatchObject({
      type: 'scoped',
      qualifier: 'language',
      languageTag: 'en-US',
      negated: false,
      value: 'cart',
    });
  });

  it('keeps wildcards in the value', () => {
    const result = parseSearchQuery('key:cart*', LANGS);
    expect(result.tokens[0]).toMatchObject({ value: 'cart*' });
  });

  it('parses quoted value with spaces and tracks positions', () => {
    const result = parseSearchQuery('description:"shopping cart" rest', LANGS);
    expect(result.tokens).toHaveLength(2);
    expect(result.tokens[0]).toMatchObject({
      type: 'scoped',
      qualifier: 'description',
      negated: false,
      value: 'shopping cart',
      from: 0,
      to: 27,
    });
    expect(result.tokens[1]).toMatchObject({
      type: 'text',
      raw: 'rest',
      from: 28,
      to: 32,
    });
  });

  it('treats unterminated quote as running to the end', () => {
    const result = parseSearchQuery('key:"a b c', LANGS);
    expect(result.tokens).toHaveLength(1);
    expect(result.tokens[0]).toMatchObject({
      type: 'scoped',
      qualifier: 'key',
      value: 'a b c',
    });
  });

  it('unquotes bare quoted phrases', () => {
    const result = parseSearchQuery('"two words"', LANGS);
    expect(result.tokens[0]).toMatchObject({
      type: 'text',
      raw: '"two words"',
      value: 'two words',
    });
  });

  it('keeps unknown qualifiers as text', () => {
    const result = parseSearchQuery('common:button.save', LANGS);
    expect(result.hasRecognizedQualifiers).toBe(false);
    expect(result.tokens[0]).toMatchObject({
      type: 'text',
      raw: 'common:button.save',
    });
  });

  it('keeps bare minus tokens as text', () => {
    const result = parseSearchQuery('-foo', LANGS);
    expect(result.tokens[0]).toMatchObject({
      type: 'text',
      raw: '-foo',
      value: '-foo',
    });
  });

  it('ignores qualifier with empty value', () => {
    const result = parseSearchQuery('key:', LANGS);
    expect(result.tokens[0]).toMatchObject({ type: 'ignored', raw: 'key:' });
    expect(result.hasRecognizedQualifiers).toBe(true);
  });

  it('ignores language qualifier with empty value', () => {
    const result = parseSearchQuery('de:', LANGS);
    expect(result.tokens[0]).toMatchObject({ type: 'ignored', raw: 'de:' });
  });

  it('ignores qualifier with whitespace-only quoted value', () => {
    const result = parseSearchQuery('description:" "', LANGS);
    expect(result.tokens[0]).toMatchObject({
      type: 'ignored',
      raw: 'description:" "',
    });
  });

  it('keeps colons inside the value', () => {
    const result = parseSearchQuery('key:foo:bar', LANGS);
    expect(result.tokens[0]).toMatchObject({
      type: 'scoped',
      qualifier: 'key',
      value: 'foo:bar',
    });
  });

  it('prefers reserved qualifier over language tag', () => {
    const result = parseSearchQuery('translation:foo', ['translation']);
    expect(result.tokens[0]).toMatchObject({
      qualifier: 'translation',
      languageTag: undefined,
    });
  });

  it('ignores language qualifiers not in the project languages', () => {
    const result = parseSearchQuery('fr:bonjour', LANGS);
    expect(result.hasRecognizedQualifiers).toBe(false);
  });

  it('parses mixed query', () => {
    const result = parseSearchQuery(
      'description:cart key:checkout* -de:Warenkorb loose',
      LANGS
    );
    expect(result.hasRecognizedQualifiers).toBe(true);
    expect(result.tokens).toHaveLength(4);
    expect(result.tokens[2]).toMatchObject({
      qualifier: 'language',
      languageTag: 'de',
      negated: true,
      value: 'Warenkorb',
    });
    expect(result.tokens[3]).toMatchObject({ type: 'text', raw: 'loose' });
  });

  it('handles empty input', () => {
    const result = parseSearchQuery('', LANGS);
    expect(result.tokens).toEqual([]);
    expect(result.hasRecognizedQualifiers).toBe(false);
  });
});
