import { useApiQuery } from 'tg.service/http/useQueryApi';

export const useTrashCount = (projectId: number, branchName?: string) => {
  const loadable = useApiQuery({
    url: '/v2/projects/{projectId}/keys/trash',
    method: 'get',
    path: { projectId },
    query: {
      size: 0,
      branch: branchName,
    },
    options: {
      staleTime: 30000,
    },
  });

  return loadable.data?.page?.totalElements ?? 0;
};
