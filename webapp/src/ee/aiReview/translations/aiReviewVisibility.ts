import { components } from 'tg.service/apiSchema.generated';

type AiReviewSummary = components['schemas']['TranslationAiReviewSummaryModel'];

export type AiReviewCellVisibility = 'badge' | 'hover' | 'none';

export const getAiReviewCellVisibility = (
  aiReview: AiReviewSummary | undefined
): AiReviewCellVisibility => {
  switch (aiReview?.status) {
    case 'CONFIRMED':
    case 'REJECTED':
      return 'hover';
    case 'APPROVED':
    case 'DRY_RUN_PASS':
    case 'DRY_RUN_FAIL':
    case 'BELOW_THRESHOLD':
    case 'FAILED':
      return 'badge';
    default:
      return 'none';
  }
};
