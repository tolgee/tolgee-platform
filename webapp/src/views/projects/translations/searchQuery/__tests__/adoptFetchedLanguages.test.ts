import { adoptFetchedLanguages } from '../adoptFetchedLanguages';

describe('adoptFetchedLanguages', () => {
  it('shaves the fetched tags by the existing selection', () => {
    expect(
      adoptFetchedLanguages({
        fetchedTags: ['en', 'de'],
        currentSelection: ['en', 'fr'],
        searchInjectedTags: ['de'],
      })
    ).toEqual(['en']);
  });

  it('adopts the fetched default set on first visit without a search', () => {
    expect(
      adoptFetchedLanguages({
        fetchedTags: ['en', 'cs'],
        currentSelection: undefined,
        searchInjectedTags: [],
      })
    ).toEqual(['en', 'cs']);
  });

  it('adopts nothing on first visit with a language-scoped search', () => {
    expect(
      adoptFetchedLanguages({
        fetchedTags: ['en', 'de'],
        currentSelection: [],
        searchInjectedTags: ['de'],
      })
    ).toBeUndefined();
  });

  it('returns an empty selection when nothing overlaps the fetched languages', () => {
    expect(
      adoptFetchedLanguages({
        fetchedTags: ['en', 'de'],
        currentSelection: ['fr'],
        searchInjectedTags: [],
      })
    ).toEqual([]);
  });
});
