import { useApiMutation, useApiQuery } from 'tg.service/http/useQueryApi';
import { UseQueryResult } from 'react-query';

export function useUserPreferenceStorage(fieldName: string) {
  const loadable = useApiQuery({
    url: '/v2/user-preferences/storage/{fieldName}',
    method: 'get',
    path: { fieldName },
  }) as UseQueryResult<{ data: Record<number, number> }>;

  const mutation = useApiMutation({
    url: '/v2/user-preferences/storage/{fieldName}',
    method: 'put',
  });

  return {
    loadable,
    update: (value: Record<string, any>) => {
      mutation.mutate({
        path: { fieldName },
        content: {
          'application/json': value,
        },
      });
    },
  };
}
