import { describe, expect, it } from 'vitest';
import {
  getAiReviewFiltersLength,
  getAiReviewFiltersName,
  toggleAiReviewOutcomes,
} from 'tg.ee.module/aiReview/translations/SubfilterAiReview';
import {
  commitAiReviewScore,
  setAiReviewScoreMode,
  setAiReviewScoreValue,
} from 'tg.ee.module/aiReview/translations/ScoreFilterRow';
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

  it('switching back to all scores clears the mode and the value', () => {
    expect(
      setAiReviewScoreMode(
        { filterAiReviewScoreMode: 'HIGHER_THAN', filterAiReviewScore: 85 },
        undefined
      )
    ).toEqual({
      filterAiReviewScoreMode: undefined,
      filterAiReviewScore: undefined,
    });
  });

  it('switching the mode keeps the typed score', () => {
    expect(
      setAiReviewScoreMode(
        { filterAiReviewScoreMode: 'HIGHER_THAN', filterAiReviewScore: 85 },
        'LOWER_THAN'
      )
    ).toEqual({
      filterAiReviewScoreMode: 'LOWER_THAN',
      filterAiReviewScore: 85,
    });
  });

  it('clamps the score to 0..100 and clears it when emptied', () => {
    const base = { filterAiReviewScoreMode: 'HIGHER_THAN' as const };
    expect(setAiReviewScoreValue(base, 150).filterAiReviewScore).toBe(100);
    expect(setAiReviewScoreValue(base, -5).filterAiReviewScore).toBe(0);
    expect(setAiReviewScoreValue(base, 42).filterAiReviewScore).toBe(42);
    expect(
      setAiReviewScoreValue({ ...base, filterAiReviewScore: 42 }, undefined)
        .filterAiReviewScore
    ).toBeUndefined();
  });

  it('selecting a mode alone keeps NOT_REVIEWED_YET', () => {
    expect(
      setAiReviewScoreMode(
        { filterAiReviewOutcomes: ['NOT_REVIEWED_YET'] },
        'HIGHER_THAN'
      ).filterAiReviewOutcomes
    ).toEqual(['NOT_REVIEWED_YET']);
  });

  it('committing a score drops NOT_REVIEWED_YET because unreviewed rows have no score', () => {
    const base = {
      filterAiReviewOutcomes: ['NOT_REVIEWED_YET', 'FAILED'] as const,
      filterAiReviewScoreMode: 'HIGHER_THAN' as const,
    };
    const next = commitAiReviewScore(
      { ...base, filterAiReviewOutcomes: [...base.filterAiReviewOutcomes] },
      85
    );
    expect(next.filterAiReviewOutcomes).toEqual(['FAILED']);
    expect(next.filterAiReviewScore).toBe(85);
    expect(
      commitAiReviewScore(
        {
          filterAiReviewOutcomes: ['NOT_REVIEWED_YET'],
          filterAiReviewScoreMode: 'HIGHER_THAN',
          filterAiReviewScore: 85,
        },
        undefined
      ).filterAiReviewOutcomes
    ).toEqual(['NOT_REVIEWED_YET']);
  });

  it('turning NOT_REVIEWED_YET on clears the score filter because unreviewed rows have no score', () => {
    const next = toggleAiReviewOutcomes(
      { filterAiReviewScoreMode: 'HIGHER_THAN', filterAiReviewScore: 85 },
      ['NOT_REVIEWED_YET']
    );
    expect(next.filterAiReviewOutcomes).toEqual(['NOT_REVIEWED_YET']);
    expect(next.filterAiReviewScoreMode).toBeUndefined();
    expect(next.filterAiReviewScore).toBeUndefined();
  });

  it('toggleAiReviewOutcomes adds and removes outcomes', () => {
    expect(
      toggleAiReviewOutcomes({ filterAiReviewOutcomes: ['FAILED'] }, [
        'OVERRIDDEN',
      ]).filterAiReviewOutcomes
    ).toEqual(['FAILED', 'OVERRIDDEN']);
    expect(
      toggleAiReviewOutcomes({ filterAiReviewOutcomes: ['FAILED'] }, ['FAILED'])
        .filterAiReviewOutcomes
    ).toBeUndefined();
  });

  it('counts the score condition only when the mode and the value are both set', () => {
    expect(
      getAiReviewFiltersLength({
        filterAiReviewScoreMode: 'HIGHER_THAN',
        filterAiReviewScore: 85,
      })
    ).toBe(1);
    expect(
      getAiReviewFiltersLength({ filterAiReviewScoreMode: 'HIGHER_THAN' })
    ).toBe(0);
    expect(
      getAiReviewFiltersLength({
        filterAiReviewOutcomes: ['FAILED'],
        filterAiReviewScoreMode: 'EQUAL_TO',
        filterAiReviewScore: 50,
      })
    ).toBe(2);
  });

  it('falls back to the score label when the restored outcome is unknown', () => {
    const name = getAiReviewFiltersName({
      filterAiReviewOutcomes: ['CONFIRMED' as AiReviewFilterOutcome],
      filterAiReviewScoreMode: 'EQUAL_TO',
      filterAiReviewScore: 0,
    });
    expect(name?.props.keyName).toBe('ai_review_filter_score_chip');
  });

  it('names an active score condition', () => {
    const name = getAiReviewFiltersName({
      filterAiReviewScoreMode: 'HIGHER_THAN',
      filterAiReviewScore: 85,
    });
    expect(name?.props.keyName).toBe('ai_review_filter_score_chip');
    expect(name?.props.params).toEqual({ mode: 'HIGHER_THAN', score: 85 });
    expect(
      getAiReviewFiltersName({ filterAiReviewScoreMode: 'HIGHER_THAN' })
    ).toBeUndefined();
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
