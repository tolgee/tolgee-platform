import {
  AiReviewRuleModel,
  AiReviewRuleRequest,
} from 'tg.ee.module/aiReview/utils/types';

export const ruleToRequest = (
  rule: AiReviewRuleModel,
  existingLanguageIds: Set<number>
): AiReviewRuleRequest => ({
  name: rule.name,
  enabled: rule.enabled,
  mode: rule.mode,
  threshold: rule.threshold,
  promptId: rule.promptId,
  skipWithOpenQaIssues: rule.skipWithOpenQaIssues,
  tagIn: rule.tagIn,
  tagNotIn: rule.tagNotIn,
  namespaceIn: rule.namespaceIn,
  namespaceNotIn: rule.namespaceNotIn,
  languageIds: rule.languageIds.filter((id) => existingLanguageIds.has(id)),
  languageThresholds: rule.languageThresholds.filter((override) =>
    existingLanguageIds.has(override.languageId)
  ),
});
