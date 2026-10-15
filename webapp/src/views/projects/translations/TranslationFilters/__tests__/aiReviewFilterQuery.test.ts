import { getAiReviewFilterQuery } from 'tg.views/projects/translations/TranslationFilters/aiReviewFilterQuery';

const filters = {
  filterAiReviewOutcome: ['FAILED' as const],
  filterAiReviewScoreBelow: 80,
  filterAiReviewRuleId: [7],
};

describe('getAiReviewFilterQuery', () => {
  it('applies to every displayed language but the base by default', () => {
    expect(
      getAiReviewFilterQuery(filters, true, ['en', 'de', 'cs'], 'en')
    ).toEqual({
      filterAiReviewOutcome: ['FAILED'],
      filterAiReviewScoreBelow: 80,
      filterAiReviewRuleId: [7],
      filterAiReviewInLang: ['de', 'cs'],
    });
  });

  it('applies to the base language too when all languages are chosen', () => {
    expect(
      getAiReviewFilterQuery(
        { ...filters, filterAiReviewApplyLanguage: true },
        true,
        ['en', 'de'],
        'en'
      ).filterAiReviewInLang
    ).toEqual(['en', 'de']);
  });

  it('sends nothing when the chosen language is not displayed', () => {
    expect(
      getAiReviewFilterQuery(
        { ...filters, filterAiReviewApplyLanguage: 'fr' },
        true,
        ['en', 'de'],
        'en'
      )
    ).toEqual({});
  });

  it('sends nothing when only the base language is displayed', () => {
    expect(getAiReviewFilterQuery(filters, true, ['en'], 'en')).toEqual({});
  });

  it('sends nothing without an AI review filter', () => {
    expect(getAiReviewFilterQuery({}, false, ['en', 'de'], 'en')).toEqual({});
  });
});
