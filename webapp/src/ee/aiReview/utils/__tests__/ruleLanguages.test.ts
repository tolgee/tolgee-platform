import {
  getRuleCoveredLanguageIds,
  getRuleRunLanguageIds,
} from 'tg.ee.module/aiReview/utils/ruleLanguages';

const rule = (
  overrides: Partial<Parameters<typeof getRuleCoveredLanguageIds>[0][number]>
) => ({
  enabled: true,
  languageIds: [] as number[],
  languageThresholds: [] as { languageId: number; never: boolean }[],
  ...overrides,
});

describe('getRuleCoveredLanguageIds', () => {
  it('rule without languages covers all candidates', () => {
    expect([...getRuleCoveredLanguageIds([rule({})], [1, 2, 3])]).toEqual([
      1, 2, 3,
    ]);
  });

  it('respects the rule languages and never overrides', () => {
    const covered = getRuleCoveredLanguageIds(
      [
        rule({
          languageIds: [1, 2],
          languageThresholds: [{ languageId: 2, never: true }],
        }),
      ],
      [1, 2, 3]
    );
    expect([...covered]).toEqual([1]);
  });

  it('ignores disabled rules and unions enabled ones', () => {
    const covered = getRuleCoveredLanguageIds(
      [
        rule({ enabled: false }),
        rule({ languageIds: [3] }),
        rule({ languageIds: [2] }),
      ],
      [1, 2, 3]
    );
    expect([...covered].sort()).toEqual([2, 3]);
  });
});

describe('getRuleRunLanguageIds', () => {
  const canChangeState = (permitted: number[]) => (id: number) =>
    permitted.includes(id);

  it('offers the rule languages the user can change the state in', () => {
    expect(
      getRuleRunLanguageIds(
        { languageIds: [1, 2] },
        [1, 2, 3],
        canChangeState([2, 3])
      )
    ).toEqual([2]);
  });

  it('rule without languages offers every permitted language', () => {
    expect(
      getRuleRunLanguageIds(
        { languageIds: [] },
        [1, 2, 3],
        canChangeState([1, 3])
      )
    ).toEqual([1, 3]);
  });

  it('offers nothing when the user can change the state in none of them', () => {
    expect(
      getRuleRunLanguageIds(
        { languageIds: [1, 2] },
        [1, 2, 3],
        canChangeState([3])
      )
    ).toEqual([]);
  });
});
