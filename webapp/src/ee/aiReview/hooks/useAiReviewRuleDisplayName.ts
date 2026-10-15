import { useTranslate } from '@tolgee/react';

import { components } from 'tg.service/apiSchema.generated';
import {
  AiReviewRuleNameSource,
  formatAiReviewRuleDisplayName,
} from 'tg.ee.module/aiReview/utils/ruleDisplayName';

type LanguageModel = components['schemas']['LanguageModel'];

export const useAiReviewRuleDisplayName = () => {
  const { t } = useTranslate();
  return (rule: AiReviewRuleNameSource, languages: LanguageModel[]) =>
    formatAiReviewRuleDisplayName(rule, languages, t);
};
