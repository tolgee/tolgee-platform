import { UseQueryResult } from 'react-query';

import { useApiMutation, useApiQuery } from 'tg.service/http/useQueryApi';
import { useProject } from 'tg.hooks/useProject';
import { useIsGuest } from 'tg.globalContext/helpers';

export function useProjectPreferenceStorage<T>(fieldName: string) {
  const project = useProject();
  const path = { projectId: project.id, fieldName };
  const isGuest = useIsGuest();

  const loadable = useApiQuery({
    url: '/v2/user-preferences/project-storage/{projectId}/{fieldName}',
    method: 'get',
    path,
    fetchOptions: {
      disable404Redirect: true,
    },
    options: {
      enabled: !isGuest,
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
    update: (value: T | null) => {
      if (isGuest) {
        return;
      }
      mutation.mutate({
        path,
        content: {
          'application/json': value as any,
        },
      });
    },
  };
}
