import { useApiMutation } from 'tg.service/http/useQueryApi';
import { useProject } from 'tg.hooks/useProject';

export const useAiReviewRun = () => {
  const project = useProject();

  const preview = useApiMutation({
    url: '/v2/projects/{projectId}/ai-review/run/preview',
    method: 'post',
  });

  const start = useApiMutation({
    url: '/v2/projects/{projectId}/ai-review/run',
    method: 'post',
    invalidatePrefix: '/v2/projects/{projectId}/ai-review-rules',
  });

  return { preview, start, projectId: project.id };
};
