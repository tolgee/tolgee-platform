import { describe, expect, it } from 'vitest';
import {
  getAiReviewFiltersLength,
  getAiReviewFiltersName,
} from 'tg.ee.module/aiReview/translations/SubfilterAiReview';
import { AiReviewFilterOutcome } from 'tg.views/projects/translations/TranslationFilters/tools';

const nameKey = (outcome: AiReviewFilterOutcome) =>
  getAiReviewFiltersName({ filterAiReviewOutcomes: [outcome] })?.props.keyName;

describe('AI review filter name', () => {
  it('names every outcome', () => {
    expect(nameKey('REVIEWED_BY_AI')).toBe('ai_review_filter_reviewed_by_ai');
    expect(nameKey('NOT_REVIEWED_YET')).toBe(
      'ai_review_filter_not_reviewed_yet'
    );
    expect(nameKey('OVERRIDDEN')).toBe('ai_review_filter_overridden');
  });

  it('has no entry for the dropped confirm and reject decisions', () => {
    for (const gone of ['CONFIRMED', 'REJECTED']) {
      expect(
        getAiReviewFiltersName({
          filterAiReviewOutcomes: [gone as AiReviewFilterOutcome],
        })
      ).toBeUndefined();
    }
  });

  it('has no name without a selected outcome', () => {
    expect(getAiReviewFiltersName({})).toBeUndefined();
  });

  it('names both score-only outcomes with the single score-only label', () => {
    expect(nameKey('SCORE_ONLY_PASS')).toBe('ai_review_filter_score_only');
    expect(nameKey('SCORE_ONLY_FAIL')).toBe('ai_review_filter_score_only');
  });

  it('counts the merged score-only selection as one filter', () => {
    expect(
      getAiReviewFiltersLength({
        filterAiReviewOutcomes: ['SCORE_ONLY_PASS', 'SCORE_ONLY_FAIL'],
      })
    ).toBe(1);
    expect(
      getAiReviewFiltersLength({
        filterAiReviewOutcomes: [
          'SCORE_ONLY_PASS',
          'SCORE_ONLY_FAIL',
          'FAILED',
        ],
      })
    ).toBe(2);
  });
});
