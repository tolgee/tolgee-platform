import { components } from 'tg.service/apiSchema.generated';

type AiReviewSummary = components['schemas']['TranslationAiReviewSummaryModel'];

export type AiReviewCellVisibility = 'badge' | 'hover' | 'none';

export const isScoreOnlyStatus = (status?: AiReviewSummary['status']) =>
  status === 'SCORE_ONLY_PASS' || status === 'SCORE_ONLY_FAIL';

export const getAiReviewCellVisibility = (
  aiReview: AiReviewSummary | undefined
): AiReviewCellVisibility => {
  if (aiReview?.queued) {
    return 'badge';
  }
  switch (aiReview?.status) {
    case 'OVERRIDDEN':
      return 'hover';
    case 'APPROVED':
    case 'SCORE_ONLY_PASS':
    case 'SCORE_ONLY_FAIL':
    case 'BELOW_THRESHOLD':
    case 'FAILED':
      return 'badge';
    default:
      return 'none';
  }
};
