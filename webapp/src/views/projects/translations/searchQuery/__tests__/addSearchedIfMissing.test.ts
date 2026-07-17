import { addSearchedIfMissing } from '../addSearchedIfMissing';

describe('addSearchedIfMissing', () => {
  it('returns languages unchanged when nothing is searched', () => {
    expect(addSearchedIfMissing(['en'], [], 'en')).toEqual(['en']);
    expect(addSearchedIfMissing(undefined, [], 'en')).toBeUndefined();
  });

  it('builds base + searched when no selection exists', () => {
    expect(addSearchedIfMissing(undefined, ['de'], 'en')).toEqual(['en', 'de']);
    expect(addSearchedIfMissing([], ['de'], 'en')).toEqual(['en', 'de']);
  });

  it('does not duplicate the base language when it is searched', () => {
    expect(addSearchedIfMissing(undefined, ['en', 'de'], 'en')).toEqual([
      'en',
      'de',
    ]);
  });

  it('returns the same selection when all searched tags are present', () => {
    const languages = ['en', 'de'];
    expect(addSearchedIfMissing(languages, ['de'], 'en')).toBe(languages);
  });

  it('appends only the missing tags', () => {
    expect(addSearchedIfMissing(['en'], ['de', 'en'], 'en')).toEqual([
      'en',
      'de',
    ]);
  });
});
