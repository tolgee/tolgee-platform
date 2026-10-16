import { useApiQuery } from 'tg.service/http/useQueryApi';
import { useAiReviewEnabled } from 'tg.ee.module/aiReview/hooks/useAiReviewEnabled';

export const useAiReviewRuleBacklogs = (projectId: number, enabled = true) => {
  const { available } = useAiReviewEnabled();
  const loadable = useApiQuery({
    url: '/v2/projects/{projectId}/ai-review/rules/backlog',
    method: 'get',
    path: { projectId },
    options: { enabled: available && enabled },
  });
  const byRuleId = new Map<number, number>(
    loadable.data?.backlogs.map((b) => [b.ruleId, b.notReviewed]) ?? []
  );
  return { byRuleId, isLoading: loadable.isLoading };
};
