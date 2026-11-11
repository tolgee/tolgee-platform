import { useTranslate } from '@tolgee/react';

import { components } from 'tg.service/apiSchema.generated';

export type SkipReason =
  keyof components['schemas']['AiReviewRunPreviewModel']['skipped'];

export const useAiReviewSkipReasonLabel = () => {
  const { t } = useTranslate();

  function skipReasonLabel(reason: SkipReason, count: number) {
    const params = { count };
    switch (reason) {
      case 'ALREADY_HAS_RESULT':
        return t(
          'ai_review_run_skip_already_has_result',
          'Already has a result: {count}',
          params
        );
      case 'ALREADY_QUEUED':
        return t(
          'ai_review_run_skip_already_queued',
          'Already queued: {count}',
          params
        );
      case 'OVERRIDDEN':
        return t(
          'ai_review_run_skip_overridden',
          'Overridden by a human: {count}',
          params
        );
      case 'HUMAN_REVIEWED':
        return t(
          'ai_review_run_skip_human_reviewed',
          'Reviewed by a human: {count}',
          params
        );
      case 'NO_RULE':
        return t(
          'ai_review_run_skip_no_rule',
          'No rule applies: {count}',
          params
        );
      case 'NEVER':
        return t(
          'ai_review_run_skip_never',
          'The rule says Never: {count}',
          params
        );
      case 'OPEN_QA_ISSUES':
        return t(
          'ai_review_run_skip_open_qa_issues',
          'Open QA issues: {count}',
          params
        );
      case 'OUTDATED':
        return t('ai_review_run_skip_outdated', 'Outdated: {count}', params);
      case 'EMPTY':
        return t('ai_review_run_skip_empty', 'Empty: {count}', params);
      case 'NO_PERMISSION':
        return t(
          'ai_review_run_skip_no_permission',
          'No permission: {count}',
          params
        );
      default:
        return `${reason}: ${count}`;
    }
  }

  return { skipReasonLabel };
};
