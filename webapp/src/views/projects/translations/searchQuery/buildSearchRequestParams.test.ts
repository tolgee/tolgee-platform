import { describe, expect, it } from 'vitest';

import {
  buildSearchRequestParams,
  getReferencedLanguageTags,
} from './buildSearchRequestParams';

const LANGS = ['en', 'de', 'en-US'];

describe('buildSearchRequestParams', () => {
  it('passes plain text through verbatim as search', () => {
    const input = '  spaced "quoted, raw" -bare unknown:qualifier ';
    expect(buildSearchRequestParams(input, LANGS)).toEqual({ search: input });
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

  it('joins remaining text tokens into search', () => {
    expect(
      buildSearchRequestParams('description:cart hello "two words"', LANGS)
    ).toEqual({
      filterDescriptionPattern: ['cart'],
      search: 'hello two words',
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
