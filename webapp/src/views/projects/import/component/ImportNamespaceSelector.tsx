import { components } from 'tg.service/apiSchema.generated';
import { useApiMutation, useApiQuery } from 'tg.service/http/useQueryApi';
import { useProject } from 'tg.hooks/useProject';
import { useImportDataHelper } from '../hooks/useImportDataHelper';
import { NamespaceSelector } from 'tg.component/NamespaceSelector/NamespaceSelector';

export const ImportNamespaceSelector = ({
  row,
}: {
  row: components['schemas']['ImportLanguageModel'];
}) => {
  const project = useProject();

  const dataHelper = useImportDataHelper();

  const mutation = useApiMutation({
    url: '/v2/projects/{projectId}/import/result/files/{fileId}/select-namespace',
    method: 'put',
    options: {
      onSuccess() {
        dataHelper.refetchData();
        namespacesLoadable.refetch();
      },
    },
  });

  const namespacesLoadable = useApiQuery({
    url: '/v2/projects/{projectId}/import/all-namespaces',
    method: 'get',
    path: { projectId: project.id },
    fetchOptions: {
      disable404Redirect: true,
    },
  });

  const currentNamespace = row.namespace || '';

  const applyNamespace = (namespace: string) => {
    mutation.mutate({
      path: { projectId: project.id, fileId: row.importFileId },
      content: {
        'application/json': {
          namespace: namespace,
        },
      },
    });
  };

  return (
    <NamespaceSelector
      value={currentNamespace}
      onChange={(ns) => applyNamespace(ns || '')}
      namespaceData={
        namespacesLoadable.data?._embedded?.namespaces?.map((n) => n.name) || []
      }
    />
  );
};
