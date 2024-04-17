import { useMemo } from 'react';
import { useApiQuery } from 'tg.service/http/useQueryApi';
import { useProject } from './useProject';
import { useProjectPermissions } from './useProjectPermissions';

export function useExportHelper() {
  const project = useProject();
  const { satisfiesLanguageAccess } = useProjectPermissions();

  const languagesLoadable = useApiQuery({
    url: '/v2/projects/{projectId}/languages',
    method: 'get',
    path: { projectId: project.id },
    query: { size: 1000 },
  });

  const namespacesLoadable = useApiQuery({
    url: '/v2/projects/{projectId}/used-namespaces',
    method: 'get',
    path: { projectId: project.id },
    fetchOptions: {
      disable404Redirect: true,
    },
  });

  const allNamespaces = useMemo(
    () =>
      namespacesLoadable.data?._embedded?.namespaces?.map((n) => n.name || ''),
    [namespacesLoadable.data]
  );

  const allowedLanguages = useMemo(
    () =>
      languagesLoadable.data?._embedded?.languages?.filter((l) =>
        satisfiesLanguageAccess('translations.view', l.id)
      ) || [],
    [languagesLoadable.data]
  );

  const allowedLanguageTags = useMemo(
    () => allowedLanguages?.map((l) => l.tag) || [],
    [allowedLanguages]
  );

  return {
    allNamespaces,
    allowedLanguageTags,
    allowedLanguages,
    isFetching: languagesLoadable.isFetching || namespacesLoadable.isFetching,
  };
}
