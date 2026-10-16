import { useTranslate } from '@tolgee/react';
import {
  AiReviewIssueSeverity,
  AiReviewOutcome,
} from 'tg.ee.module/aiReview/utils/types';
import { AiReviewFilterOutcome } from 'tg.views/projects/translations/TranslationFilters/tools';

export const useAiReviewOutcomeLabel = () => {
  const { t } = useTranslate();
  return (outcome: AiReviewOutcome, dryRun = false) => {
    switch (outcome) {
      case 'AUTO_REVIEWED':
        return t('ai_review_outcome_auto_reviewed', 'Reviewed by AI');
      case 'WOULD_AUTO_REVIEW':
        return t(
          'ai_review_outcome_would_auto_review',
          'Dry run: would be auto-reviewed'
        );
      case 'BELOW_THRESHOLD':
        return dryRun
          ? t(
              'ai_review_outcome_would_not_auto_review',
              'Dry run: would not be auto-reviewed'
            )
          : t('ai_review_outcome_below_threshold', 'Below threshold');
      case 'FAILED':
        return t('ai_review_outcome_failed', 'AI review failed');
      case 'CONFIRMED':
        return t('ai_review_outcome_confirmed', 'AI review confirmed');
      case 'REJECTED':
        return t('ai_review_outcome_rejected', 'AI review rejected');
      case 'PENDING':
        return t('ai_review_outcome_pending', 'AI review pending');
    }
  };
};

export const useAiReviewSeverityLabel = () => {
  const { t } = useTranslate();
  return (severity: AiReviewIssueSeverity) => {
    switch (severity) {
      case 'CRITICAL':
        return t('ai_review_severity_critical', 'Critical');
      case 'MAJOR':
        return t('ai_review_severity_major', 'Major');
      case 'MINOR':
        return t('ai_review_severity_minor', 'Minor');
    }
  };
};

export const useAiReviewFilterOutcomeLabel = () => {
  const { t } = useTranslate();
  return (outcome: AiReviewFilterOutcome) => {
    switch (outcome) {
      case 'AUTO_REVIEWED':
        return t('ai_review_outcome_auto_reviewed', 'Reviewed by AI');
      case 'BELOW_THRESHOLD':
        return t('ai_review_outcome_below_threshold', 'Below threshold');
      case 'WOULD_AUTO_REVIEW':
        return t(
          'ai_review_outcome_would_auto_review',
          'Dry run: would be auto-reviewed'
        );
      case 'WOULD_NOT_AUTO_REVIEW':
        return t('ai_review_filter_would_not', 'Dry run: would not');
      case 'FAILED':
        return t('ai_review_filter_failed', 'Failed');
      case 'NOT_REVIEWED':
        return t('ai_review_filter_not_reviewed', 'Not reviewed yet');
    }
  };
};
