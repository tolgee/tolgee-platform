import { getSuggestions } from '../searchSuggestions';
import { pickLanguageExampleTag } from '../qualifierHints';

const LANGS = ['en', 'de', 'en-US'];

const inserts = (value: string, caret: number, langs = LANGS) =>
  getSuggestions(value, caret, langs)?.items.map((i) => i.insert);

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

  it('does not suggest language tags outside the qualifier charset', () => {
    expect(inserts('zh', 2, ['zh.Hans', 'zh-Hant'])).toEqual(['zh-Hant:']);
  });

  it('does not suggest language tags shadowed by reserved qualifiers', () => {
    expect(inserts('trans', 5, ['translation', 'de'])).toEqual([
      'translation:',
    ]);
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
    expect(state.replaceTo).toBe(4);
  });

  it('returns nothing for tokens with a qualifier already', () => {
    expect(getSuggestions('description:foo', 15, LANGS)).toBeUndefined();
  });

  it('keeps the exactly matching option while its value is empty', () => {
    expect(inserts('de:', 3)).toEqual(['de:']);
    expect(inserts('description:', 12)).toEqual(['description:']);
    expect(inserts('-de:', 4)).toEqual(['de:']);
    const state = getSuggestions('-de:', 4, LANGS)!;
    expect(state.replaceFrom).toBe(1);
    expect(state.replaceTo).toBe(4);
  });

  it('hides the exact match once the caret leaves the token end', () => {
    expect(getSuggestions('de: foo', 3, LANGS)?.items).toHaveLength(1);
    expect(getSuggestions('de:foo', 6, LANGS)).toBeUndefined();
  });

  it('returns nothing inside quotes', () => {
    expect(getSuggestions('key:"des', 8, LANGS)).toBeUndefined();
    expect(getSuggestions('"des', 4, LANGS)).toBeUndefined();
  });

  it('returns nothing without matches', () => {
    expect(getSuggestions('xyz', 3, LANGS)).toBeUndefined();
  });

  it('computes replace range for a mid-value token', () => {
    const state = getSuggestions('des something', 3, LANGS)!;
    expect(state.items[0].insert).toBe('description:');
    expect(state.replaceFrom).toBe(0);
    expect(state.replaceTo).toBe(3);
  });
});

describe('pickLanguageExampleTag', () => {
  it('skips tags outside the qualifier charset', () => {
    expect(pickLanguageExampleTag(['zh.Hans', 'cs'])).toBe('cs');
  });

  it('falls back to de when no tag is usable', () => {
    expect(pickLanguageExampleTag(['zh.Hans'])).toBe('de');
    expect(pickLanguageExampleTag([])).toBe('de');
  });

  it('skips tags shadowed by reserved qualifiers', () => {
    expect(pickLanguageExampleTag(['translation', 'cs'])).toBe('cs');
  });
});
