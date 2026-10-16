type RuleLanguages = {
  enabled: boolean;
  languageIds: number[];
  languageThresholds: { languageId: number; never: boolean }[];
};

export const getRuleCoveredLanguageIds = (
  rules: RuleLanguages[],
  candidateLanguageIds: number[]
): Set<number> => {
  const covered = new Set<number>();
  rules
    .filter((rule) => rule.enabled)
    .forEach((rule) => {
      const never = new Set(
        rule.languageThresholds.filter((o) => o.never).map((o) => o.languageId)
      );
      candidateLanguageIds
        .filter(
          (id) =>
            (rule.languageIds.length === 0 || rule.languageIds.includes(id)) &&
            !never.has(id)
        )
        .forEach((id) => covered.add(id));
    });
  return covered;
};

export const getRuleRunLanguageIds = (
  rule: { languageIds: number[] },
  nonBaseLanguageIds: number[],
  canChangeState: (languageId: number) => boolean
): number[] =>
  nonBaseLanguageIds.filter(
    (id) =>
      (rule.languageIds.length === 0 || rule.languageIds.includes(id)) &&
      canChangeState(id)
  );
