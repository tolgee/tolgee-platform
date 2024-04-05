import { useEffect, useMemo } from 'react';
import { useProject } from './useProject';
import { useApiQuery } from 'tg.service/http/useQueryApi';

export const useProjectNamespaces = () => {
  const projectDTO = useProject();

  const namespacesLoadable = useApiQuery({
    url: '/v2/projects/{projectId}/used-namespaces',
    method: 'get',
    path: { projectId: projectDTO.id },
  });

  const allNamespaces = namespacesLoadable?.data?._embedded?.namespaces || [];

  const allNamespacesWithNone = useMemo(() => {
    if (allNamespaces.length === 0) {
      return [{ id: 0, name: '<none>' }];
    }
    if (allNamespaces.every((namespace) => !!namespace.name)) {
      return [{ id: 0, name: '<none>' }, ...allNamespaces];
    }
    return allNamespaces;
  }, [allNamespaces]);

  const defaultNamespace = projectDTO.defaultNamespace;

  useEffect(() => {
    // reset namespaces when unmount
    return () => {
      namespacesLoadable.remove();
    };
  }, []);

  return {
    allNamespacesWithNone,
    defaultNamespace,
  };
};
