import { components } from 'tg.service/apiSchema.generated';

export type AiReviewRuleModel = components['schemas']['AiReviewRuleModel'];
export type AiReviewRuleRequest = components['schemas']['AiReviewRuleRequest'];
export type AiReviewLanguageThreshold =
  components['schemas']['AiReviewLanguageThreshold'];
export type AiReviewIssueModel = components['schemas']['AiReviewIssueModel'];
export type TranslationAiReviewModel =
  components['schemas']['TranslationAiReviewModel'];
export type TranslationAiReviewSummaryModel =
  components['schemas']['TranslationAiReviewSummaryModel'];
export type AiReviewOutcome = TranslationAiReviewModel['outcome'];
export type AiReviewIssueSeverity = AiReviewIssueModel['severity'];
export type AiReviewRuleMode = AiReviewRuleModel['mode'];
export type AiReviewRunCounts = components['schemas']['AiReviewRunCounts'];
