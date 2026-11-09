import { useTranslate } from '@tolgee/react';

import { components } from 'tg.service/apiSchema.generated';

type AiReviewStatus =
  components['schemas']['TranslationAiReviewSummaryModel']['status'];
type FailureReason = NonNullable<
  components['schemas']['TranslationAiReviewModel']['failureReason']
>;

export const useAiReviewStatusLabels = () => {
  const { t } = useTranslate();

  function statusLabel(status: AiReviewStatus) {
    switch (status) {
      case 'APPROVED':
        return t('ai_review_popover_status_approved', 'Reviewed by AI');
      case 'BELOW_THRESHOLD':
        return t('ai_review_popover_status_below', 'Below threshold');
      case 'DRY_RUN_PASS':
        return t(
          'ai_review_popover_status_dry_run_pass',
          'Dry run: would be auto-reviewed'
        );
      case 'DRY_RUN_FAIL':
        return t(
          'ai_review_popover_status_dry_run_fail',
          'Dry run: would not be auto-reviewed'
        );
      case 'FAILED':
        return t('ai_review_popover_status_failed', 'Review failed');
      case 'CONFIRMED':
        return t('ai_review_popover_status_confirmed', 'Confirmed by a human');
      case 'REJECTED':
        return t('ai_review_popover_status_rejected', 'Rejected by a human');
      default:
        return status;
    }
  }

  function failureReasonLabel(reason: FailureReason) {
    switch (reason) {
      case 'NO_PROVIDER':
        return t(
          'ai_review_failure_no_provider',
          'No AI provider is configured'
        );
      case 'PROVIDER_ERROR':
        return t(
          'ai_review_failure_provider_error',
          'The AI provider returned an error'
        );
      case 'OUT_OF_CREDITS':
        return t('ai_review_failure_out_of_credits', 'Out of AI credits');
      case 'TEMPLATE_ERROR':
        return t(
          'ai_review_failure_template_error',
          'The prompt template failed'
        );
      case 'PARSE_ERROR':
        return t(
          'ai_review_failure_parse_error',
          'The AI response could not be read'
        );
      default:
        return reason;
    }
  }

  return { statusLabel, failureReasonLabel };
};
