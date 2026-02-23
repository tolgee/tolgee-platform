import { useApiQuery } from 'tg.service/http/useQueryApi';

export const useTrashCount = (projectId: number) => {
  // @ts-ignore - trash endpoints will be typed after schema regeneration
  const loadable = useApiQuery({
    url: '/v2/projects/{projectId}/keys/trash',
    method: 'get',
    path: { projectId },
    query: {
      size: 0,
    },
    options: {
      staleTime: 30000,
    },
  });

  return (loadable.data as any)?.page?.totalElements ?? 0;
};
