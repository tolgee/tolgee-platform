import { describe, expect, it } from 'vitest';

import { withoutDanglingLanguageScopes } from './useTranslationFilters';

describe('withoutDanglingLanguageScopes', () => {
  it('drops only the scopes whose language is gone', () => {
    expect(
      withoutDanglingLanguageScopes(
        {
          filterTaskLanguage: 'fr',
          filterTranslationLanguage: 'de',
          filterTag: ['keep-me'],
        },
        ['de']
      )
    ).toEqual({
      filterTaskLanguage: undefined,
      filterTranslationLanguage: 'de',
      filterTag: ['keep-me'],
    });
  });

  it('leaves the filters alone when every language is still selected', () => {
    expect(
      withoutDanglingLanguageScopes({ filterTaskLanguage: 'fr' }, ['fr', 'de'])
    ).toBeUndefined();
  });

  it('ignores the all-languages scope, which names no language', () => {
    expect(
      withoutDanglingLanguageScopes({ filterTaskLanguage: true }, ['de'])
    ).toBeUndefined();
  });

  it('does nothing while the language selection is unknown', () => {
    // the translations view sends undefined until the server resolves the selection, and the
    // scoped filters must survive that window
    expect(
      withoutDanglingLanguageScopes({ filterTaskLanguage: 'fr' }, undefined)
    ).toBeUndefined();
  });

  it('returns a new object rather than mutating the one it was given', () => {
    const filters = { filterTaskLanguage: 'fr' } as const;
    const updated = withoutDanglingLanguageScopes(filters, ['de']);
    expect(updated).not.toBe(filters);
    expect(filters.filterTaskLanguage).toBe('fr');
  });
});
