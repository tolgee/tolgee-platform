import { useApiQuery } from 'tg.service/http/useQueryApi';

export const PermissionsAdvanced = () => {
  const hierarchy = useApiQuery({
    url: '/v2/public/scope-info/hierarchy',
    method: 'get',
    query: {},
  });

  return <pre>{JSON.stringify(hierarchy.data, null, 2)}</pre>;
};
