import { useProject } from 'tg.hooks/useProject';

import { useAiReviewFeatureEnabled } from 'tg.ee.module/aiReview/hooks/useAiReviewFeatureEnabled';

export const useAiReviewActive = () => {
  const project = useProject();
  const aiReviewFeatureEnabled = useAiReviewFeatureEnabled();
  return aiReviewFeatureEnabled && project.useAiReview;
};
