import {
  buildSearchRequestParams,
  getReferencedLanguageTags,
} from '../buildSearchRequestParams';

const LANGS = ['en', 'de', 'en-US'];

describe('buildSearchRequestParams', () => {
  it('passes plain text through verbatim as search', () => {
    const input = '  spaced -bare unknown:qualifier ';
    expect(buildSearchRequestParams(input, LANGS)).toEqual({ search: input });
  });

  it('unquotes only qualifier-shaped quoted terms so quoting escapes qualifiers', () => {
    expect(buildSearchRequestParams('"key:foo" other', LANGS)).toEqual({
      search: 'key:foo other',
    });
  });

  it('keeps ordinary quote-containing searches verbatim', () => {
    expect(buildSearchRequestParams('Click "Save"', LANGS)).toEqual({
      search: 'Click "Save"',
    });
  });

  it('returns undefined search for empty input', () => {
    expect(buildSearchRequestParams('', LANGS)).toEqual({ search: undefined });
    expect(buildSearchRequestParams(undefined, LANGS)).toEqual({
      search: undefined,
    });
  });

  it('maps qualifiers to pattern params', () => {
    expect(
      buildSearchRequestParams(
        'key:cart* description:legal namespace:web translation:foo de:Warenkorb',
        LANGS
      )
    ).toEqual({
      filterKeyPattern: ['cart*'],
      filterDescriptionPattern: ['legal'],
      filterNamespacePattern: ['web'],
      filterTranslationPattern: ['*,foo', 'de,Warenkorb'],
      search: undefined,
    });
  });

  it('maps negated qualifiers to negated params', () => {
    expect(
      buildSearchRequestParams(
        '-key:internal_* -description:legacy -namespace:web -translation:draft -de:Warenkorb',
        LANGS
      )
    ).toEqual({
      filterNoKeyPattern: ['internal_*'],
      filterNoDescriptionPattern: ['legacy'],
      filterNoNamespacePattern: ['web'],
      filterNoTranslationPattern: ['*,draft', 'de,Warenkorb'],
      search: undefined,
    });
  });

  it('collects repeated qualifiers', () => {
    expect(buildSearchRequestParams('key:cart* key:*title', LANGS)).toEqual({
      filterKeyPattern: ['cart*', '*title'],
      search: undefined,
    });
  });

  it('joins remaining text tokens into search as typed', () => {
    expect(
      buildSearchRequestParams('description:cart hello "two words"', LANGS)
    ).toEqual({
      filterDescriptionPattern: ['cart'],
      search: 'hello "two words"',
    });
  });

  it('unquotes qualifier-escape tokens also next to real qualifiers', () => {
    expect(
      buildSearchRequestParams('description:cart "key:foo"', LANGS)
    ).toEqual({
      filterDescriptionPattern: ['cart'],
      search: 'key:foo',
    });
  });

  it('uses canonical language tag in the param value', () => {
    expect(buildSearchRequestParams('EN-us:cart', LANGS)).toEqual({
      filterTranslationPattern: ['en-US,cart'],
      search: undefined,
    });
  });

  it('keeps commas in translation pattern values', () => {
    expect(buildSearchRequestParams('en:"cart, with comma"', LANGS)).toEqual({
      filterTranslationPattern: ['en,cart, with comma'],
      search: undefined,
    });
  });

  it('applies no filter for a qualifier with empty value', () => {
    expect(buildSearchRequestParams('description:', LANGS)).toEqual({
      search: undefined,
    });
    expect(buildSearchRequestParams('description: cart', LANGS)).toEqual({
      search: 'cart',
    });
  });

  it('keeps patterns exactly at the backend limits as filters', () => {
    const maxLength = 'a'.repeat(500);
    expect(buildSearchRequestParams(`key:${maxLength}`, LANGS)).toEqual({
      filterKeyPattern: [maxLength],
      search: undefined,
    });
    expect(buildSearchRequestParams('key:*a*b*c*d*e', LANGS)).toEqual({
      filterKeyPattern: ['*a*b*c*d*e'],
      search: undefined,
    });
  });

  it('demotes tokens exceeding the backend limits to plain text terms', () => {
    const long = 'a'.repeat(501);
    expect(
      buildSearchRequestParams(`key:${long} description:ok`, LANGS)
    ).toEqual({
      filterDescriptionPattern: ['ok'],
      search: `key:${long}`,
    });
    expect(buildSearchRequestParams('key:*a*b*c*d*e*f de:x', LANGS)).toEqual({
      filterTranslationPattern: ['de,x'],
      search: 'key:*a*b*c*d*e*f',
    });
  });

  it('ignores the any-language marker when counting wildcard limits', () => {
    // the pattern part holds exactly 5 wildcards — the backend accepts it
    expect(
      buildSearchRequestParams('translation:*,a*b*c*d*e*f', LANGS)
    ).toEqual({
      filterTranslationPattern: ['*,a*b*c*d*e*f'],
      search: undefined,
    });
    expect(
      buildSearchRequestParams('translation:*,a*b*c*d*e*f*g', LANGS)
    ).toEqual({
      search: 'translation:*,a*b*c*d*e*f*g',
    });
    expect(buildSearchRequestParams('de:*a*b*c*d*e*f', LANGS)).toEqual({
      search: 'de:*a*b*c*d*e*f',
    });
  });

  it('demotes qualifiers over the backend list limit to plain text terms', () => {
    const input = Array.from({ length: 21 }, (_, i) => `key:p${i}`).join(' ');
    const result = buildSearchRequestParams(input, LANGS);
    expect(result.filterKeyPattern).toHaveLength(20);
    expect(result.search).toBe('key:p20');
  });

  it('treats a quoted translation value as a literal phrase, never shorthand', () => {
    expect(buildSearchRequestParams('translation:"de,luxe"', LANGS)).toEqual({
      filterTranslationPattern: ['*,de,luxe'],
      search: undefined,
    });
  });

  it('accepts the canonical translation qualifier with a language prefix', () => {
    expect(buildSearchRequestParams('translation:de,Warenkorb', LANGS)).toEqual(
      {
        filterTranslationPattern: ['de,Warenkorb'],
        search: undefined,
      }
    );
    expect(buildSearchRequestParams('translation:*,cart', LANGS)).toEqual({
      filterTranslationPattern: ['*,cart'],
      search: undefined,
    });
    expect(buildSearchRequestParams('translation:hello, world', LANGS)).toEqual(
      {
        filterTranslationPattern: ['*,hello,'],
        search: 'world',
      }
    );
    expect(buildSearchRequestParams('translation:xx,cart', LANGS)).toEqual({
      filterTranslationPattern: ['*,xx,cart'],
      search: undefined,
    });
    expect(buildSearchRequestParams('translation:EN-us,foo', LANGS)).toEqual({
      filterTranslationPattern: ['en-US,foo'],
      search: undefined,
    });
  });
});

describe('getReferencedLanguageTags', () => {
  it('extracts concrete language tags from translation patterns', () => {
    const params = buildSearchRequestParams(
      'de:foo -en-US:bar translation:baz de:again',
      LANGS
    );
    expect(getReferencedLanguageTags(params)).toEqual(['de', 'en-US']);
  });

  it('returns empty array without translation patterns', () => {
    expect(
      getReferencedLanguageTags(buildSearchRequestParams('key:foo', LANGS))
    ).toEqual([]);
  });
});
