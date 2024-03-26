import { useEffect } from 'react';
import { useProject } from './useProject';
import { useApiMutation, useApiQuery } from 'tg.service/http/useQueryApi';

export const useProjectNamespaces = () => {
  const projectDTO = useProject();

  const namespacesLoadable = useApiQuery({
    url: '/v2/projects/{projectId}/used-namespaces',
    method: 'get',
    path: { projectId: projectDTO.id },
  });

  const namespaces = namespacesLoadable?.data?._embedded?.namespaces || [];

  const baseNamespace = namespaces.find((namespace) => namespace.base);

  const namespaceUpdate = useApiMutation({
    url: '/v2/projects/{projectId}/namespaces/{id}',
    method: 'put',
    invalidatePrefix: '/v2/projects/{projectId}/',
  });

  useEffect(() => {
    // reset namespaces when unmount
    return () => {
      namespacesLoadable.remove();
    };
  }, []);

  return {
    namespaces,
    baseNamespace,
    namespaceUpdate,
  };
};
