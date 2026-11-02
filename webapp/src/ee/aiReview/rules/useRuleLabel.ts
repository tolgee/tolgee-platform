import { useTranslate } from '@tolgee/react';
import { components } from 'tg.service/apiSchema.generated';

type AiReviewRuleModel = components['schemas']['AiReviewRuleModel'];

export const useRuleLabel = () => {
  const { t } = useTranslate();
  return (rule: AiReviewRuleModel): string => {
    if (rule.name) {
      return rule.name;
    }
    const tags = rule.tagIn.filter(Boolean).join(', ');
    if (tags) {
      return t('ai_review_rule_label_tags', 'Tags {tags}', { tags });
    }
    const namespaces = rule.namespaceIn.filter(Boolean).join(', ');
    if (namespaces) {
      return t('ai_review_rule_label_namespaces', 'Namespaces {namespaces}', {
        namespaces,
      });
    }
    return t('ai_review_rule_label_all_keys', 'All keys');
  };
};
