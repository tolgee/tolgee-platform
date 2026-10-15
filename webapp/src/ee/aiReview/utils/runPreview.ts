import { components } from 'tg.service/apiSchema.generated';

type AiReviewRunPreviewModel = components['schemas']['AiReviewRunPreviewModel'];

export const LARGE_RUN_THRESHOLD = 5000;

export type RunPreviewState = ReturnType<typeof getRunPreviewState>;

export const getRunPreviewState = (
  preview: AiReviewRunPreviewModel,
  rereview: boolean
) => {
  const counts = rereview ? preview.withRereview : preview.withoutRereview;
  const willReview = counts.willReview;
  return {
    counts,
    willReview,
    skipped: preview.total - willReview,
    rereviewAdds:
      preview.withRereview.willReview - preview.withoutRereview.willReview,
    nothingToReview: willReview === 0,
    isLarge: willReview >= LARGE_RUN_THRESHOLD,
  };
};
