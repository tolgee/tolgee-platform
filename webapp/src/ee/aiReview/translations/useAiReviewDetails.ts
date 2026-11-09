import { useApiQuery } from 'tg.service/http/useQueryApi';
import { useProject } from 'tg.hooks/useProject';

export const useAiReviewDetails = (translationId: number) => {
  const project = useProject();
  return useApiQuery({
    url: '/v2/projects/{projectId}/translations/{translationId}/ai-review',
    method: 'get',
    path: { projectId: project.id, translationId },
    options: { noGlobalLoading: true },
  });
};
