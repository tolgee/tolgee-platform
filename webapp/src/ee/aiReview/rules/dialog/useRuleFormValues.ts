import { components } from 'tg.service/apiSchema.generated';

type AiReviewRuleModel = components['schemas']['AiReviewRuleModel'];
type AiReviewRuleRequest = components['schemas']['AiReviewRuleRequest'];

const DEFAULT_THRESHOLD = 85;

export type RuleFormValues = {
  name: string;
  tagIn: string[];
  tagNotIn: string[];
  namespaceIn: string[];
  namespaceNotIn: string[];
  languageIds: number[];
  /** Distinct from an empty `languageIds`, which means all languages. */
  noLanguages: boolean;
  branchIn: string[];
  branchNotIn: string[];
  threshold: number | string;
  never: boolean;
  mode: AiReviewRuleModel['mode'];
  promptId: number | '';
  skipOpenQaIssues: boolean;
};

export const getRuleInitialValues = (
  rule?: AiReviewRuleModel
): RuleFormValues => ({
  name: rule?.name ?? '',
  tagIn: rule?.tagIn ?? [],
  tagNotIn: rule?.tagNotIn ?? [],
  namespaceIn: rule?.namespaceIn ?? [],
  namespaceNotIn: rule?.namespaceNotIn ?? [],
  languageIds: rule?.languageIds ?? [],
  noLanguages: false,
  branchIn: rule?.branchIn ?? [],
  branchNotIn: rule?.branchNotIn ?? [],
  threshold: rule?.threshold ?? DEFAULT_THRESHOLD,
  never: rule?.never ?? false,
  mode: rule?.mode ?? 'SCORE_ONLY',
  promptId: rule?.promptId ?? '',
  skipOpenQaIssues: rule?.skipOpenQaIssues ?? true,
});

export const ruleValuesToRequest = (
  values: RuleFormValues,
  enabled: boolean
): AiReviewRuleRequest => ({
  name: values.name,
  enabled,
  tagIn: values.tagIn,
  tagNotIn: values.tagNotIn,
  namespaceIn: values.namespaceIn,
  namespaceNotIn: values.namespaceNotIn,
  languageIds: values.languageIds,
  branchIn: values.branchIn,
  branchNotIn: values.branchNotIn,
  threshold: toThreshold(values.threshold),
  never: values.never,
  mode: values.mode,
  promptId: values.promptId === '' ? undefined : values.promptId,
  skipOpenQaIssues: values.skipOpenQaIssues,
});

function toThreshold(value: number | string): number {
  const parsed = Number(value);
  return Number.isFinite(parsed) && value !== '' ? parsed : DEFAULT_THRESHOLD;
}
