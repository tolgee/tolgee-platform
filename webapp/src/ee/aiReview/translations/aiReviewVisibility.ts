import { components } from 'tg.service/apiSchema.generated';

type AiReviewSummary = components['schemas']['TranslationAiReviewSummaryModel'];
type State = components['schemas']['TranslationViewModel']['state'];

export const isAiReviewBadgeVisible = (
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
