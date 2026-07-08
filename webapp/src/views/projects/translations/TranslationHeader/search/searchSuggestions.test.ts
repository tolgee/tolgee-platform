import { describe, expect, it } from 'vitest';

import { applySuggestion, getSuggestions } from './searchSuggestions';

const LANGS = ['en', 'de', 'en-US'];

const inserts = (value: string, caret: number) =>
  getSuggestions(value, caret, LANGS)?.items.map((i) => i.insert);

describe('getSuggestions', () => {
  it('suggests all qualifiers and languages on empty input', () => {
    expect(inserts('', 0)).toEqual([
      'key:',
      'description:',
      'namespace:',
      'translation:',
      'en:',
      'de:',
      'en-US:',
    ]);
  });

  it('filters by token prefix case-insensitively', () => {
    expect(inserts('DES', 3)).toEqual(['description:']);
  });

  it('suggests language tags', () => {
    expect(inserts('some en', 7)).toEqual(['en:', 'en-US:']);
  });

  it('completes the token under the caret only', () => {
    const state = getSuggestions('foo des bar', 7, LANGS)!;
    expect(state.replaceFrom).toBe(4);
    expect(state.replaceTo).toBe(7);
  });

  it('keeps negation prefix out of the replace range', () => {
    const state = getSuggestions('-des', 4, LANGS)!;
    expect(state.items[0].insert).toBe('description:');
    expect(state.replaceFrom).toBe(1);
    expect(applySuggestion('-des', state, state.items[0])).toEqual({
      value: '-description:',
      caret: 13,
    });
  });

  it('returns nothing for tokens with a qualifier already', () => {
    expect(getSuggestions('description:foo', 15, LANGS)).toBeUndefined();
  });

  it('returns nothing inside quotes', () => {
    expect(getSuggestions('key:"des', 8, LANGS)).toBeUndefined();
  });

  it('returns nothing without matches', () => {
    expect(getSuggestions('xyz', 3, LANGS)).toBeUndefined();
  });

  it('replaces mid-value token and moves caret after the colon', () => {
    const state = getSuggestions('des something', 3, LANGS)!;
    expect(applySuggestion('des something', state, state.items[0])).toEqual({
      value: 'description: something',
      caret: 12,
    });
  });
});
