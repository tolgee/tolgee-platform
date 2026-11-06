import { components } from 'tg.service/apiSchema.generated';

type AiReviewSummary = components['schemas']['TranslationAiReviewSummaryModel'];
type State = components['schemas']['TranslationViewModel']['state'];

export type AiReviewCellVisibility = 'badge' | 'hover' | 'none';

export const getAiReviewCellVisibility = (
  aiReview: AiReviewSummary | undefined,
  state: State | undefined,
  auto: boolean | undefined
): AiReviewCellVisibility => {
  if (!aiReview) {
    return 'none';
  }
  if (aiReview.status === 'CONFIRMED' || aiReview.status === 'REJECTED') {
    return 'hover';
  }
  return isAiReviewBadgeVisible(aiReview, state, auto) ? 'badge' : 'none';
};

const isAiReviewBadgeVisible = (
  aiReview: AiReviewSummary | undefined,
  state: State | undefined,
  auto: boolean | undefined
): boolean => {
  if (!aiReview) {
    return false;
  }
  switch (aiReview.status) {
    case 'APPROVED':
    case 'DRY_RUN_PASS':
    case 'DRY_RUN_FAIL':
      return true;
    case 'BELOW_THRESHOLD':
      return state === 'TRANSLATED' || (state === 'REVIEWED' && !!auto);
    case 'FAILED':
      return state === 'TRANSLATED';
    default:
      return false;
  }
};
