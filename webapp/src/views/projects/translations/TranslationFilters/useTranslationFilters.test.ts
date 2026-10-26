import { describe, expect, it } from 'vitest';

import {
  toggleAutoTranslated,
  toggleAutoTranslatedBy,
  withoutDanglingLanguageScopes,
} from 'tg.views/projects/translations/TranslationFilters/useTranslationFilters';

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

describe('toggleAutoTranslatedBy', () => {
  it('selects auto-translated narrowed to the source when nothing was selected', () => {
    expect(
      toggleAutoTranslatedBy(
        { filterTranslationState: ['REVIEWED'] },
        'TRANSLATION_MEMORY'
      )
    ).toEqual({
      filterTranslationState: ['REVIEWED', 'AUTO_TRANSLATED'],
      filterAutoTranslatedBy: 'TRANSLATION_MEMORY',
    });
  });

  it('widens back to any source when the other source is checked too', () => {
    expect(
      toggleAutoTranslatedBy(
        {
          filterTranslationState: ['AUTO_TRANSLATED'],
          filterAutoTranslatedBy: 'TRANSLATION_MEMORY',
        },
        'MACHINE_TRANSLATION'
      )
    ).toEqual({
      filterTranslationState: ['AUTO_TRANSLATED'],
      filterAutoTranslatedBy: undefined,
    });
  });

  it('narrows to the other source when one is unchecked from any source', () => {
    expect(
      toggleAutoTranslatedBy(
        { filterTranslationState: ['AUTO_TRANSLATED'] },
        'MACHINE_TRANSLATION'
      )
    ).toEqual({
      filterTranslationState: ['AUTO_TRANSLATED'],
      filterAutoTranslatedBy: 'TRANSLATION_MEMORY',
    });
  });

  it('drops the auto-translated filter when its only source is unchecked', () => {
    expect(
      toggleAutoTranslatedBy(
        {
          filterTranslationState: ['REVIEWED', 'AUTO_TRANSLATED'],
          filterAutoTranslatedBy: 'TRANSLATION_MEMORY',
        },
        'TRANSLATION_MEMORY'
      )
    ).toEqual({
      filterTranslationState: ['REVIEWED'],
      filterAutoTranslatedBy: undefined,
    });
  });
});

describe('toggleAutoTranslated', () => {
  it('selects any source when it was off', () => {
    expect(
      toggleAutoTranslated({ filterAutoTranslatedBy: 'TRANSLATION_MEMORY' })
    ).toEqual({
      filterTranslationState: ['AUTO_TRANSLATED'],
      filterAutoTranslatedBy: undefined,
    });
  });

  it('selects any source when only one source was selected', () => {
    expect(
      toggleAutoTranslated({
        filterTranslationState: ['AUTO_TRANSLATED'],
        filterAutoTranslatedBy: 'MACHINE_TRANSLATION',
      })
    ).toEqual({
      filterTranslationState: ['AUTO_TRANSLATED'],
      filterAutoTranslatedBy: undefined,
    });
  });

  it('clears it when any source was selected', () => {
    expect(
      toggleAutoTranslated({
        filterTranslationState: ['REVIEWED', 'AUTO_TRANSLATED'],
      })
    ).toEqual({
      filterTranslationState: ['REVIEWED'],
      filterAutoTranslatedBy: undefined,
    });
  });
});
