import { useTranslate } from '@tolgee/react';
import { FeatureFlag } from 'tg.service/apiSchemaTypes';

export function useFeatureFlagLabels() {
  const { t } = useTranslate();
  return {
    AI_REVIEW: t('feature_flag_ai_review', 'AI review'),
  } as const satisfies Record<FeatureFlag, string>;
}
