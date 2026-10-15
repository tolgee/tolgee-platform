import { useApiQuery } from 'tg.service/http/useQueryApi';
import { useAiReviewEnabled } from 'tg.ee.module/aiReview/hooks/useAiReviewEnabled';

export const useAiReviewRules = (projectId: number, enabled = true) => {
  const { available } = useAiReviewEnabled();
  return useApiQuery({
    url: '/v2/projects/{projectId}/ai-review/rules',
    method: 'get',
    path: { projectId },
    options: { enabled: available && enabled },
  });
};
