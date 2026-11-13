import { UseQueryResult } from 'react-query';

import { useApiMutation, useApiQuery } from 'tg.service/http/useQueryApi';
import { useProject } from 'tg.hooks/useProject';

export function useProjectPreferenceStorage<T>(fieldName: string) {
  const project = useProject();
  const path = { projectId: project.id, fieldName };

  const loadable = useApiQuery({
    url: '/v2/user-preferences/project-storage/{projectId}/{fieldName}',
    method: 'get',
    path,
    fetchOptions: {
      disable404Redirect: true,
    },
    options: {
      cacheTime: 0,
      refetchOnWindowFocus: false,
    },
  }) as UseQueryResult<{ data?: T | null }>;

  const mutation = useApiMutation({
    url: '/v2/user-preferences/project-storage/{projectId}/{fieldName}',
    method: 'put',
  });

  return {
    loadable,
    update: (value: T | null) =>
      mutation.mutate({
        path,
        content: {
          'application/json': value as any,
        },
      }),
  };
}
