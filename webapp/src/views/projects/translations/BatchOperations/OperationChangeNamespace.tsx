import { useState } from 'react';

import { NamespaceSelector } from 'tg.component/NamespaceSelector/NamespaceSelector';
import { useApiMutation } from 'tg.service/http/useQueryApi';
import { useProject } from 'tg.hooks/useProject';

import { useTranslationsSelector } from '../context/TranslationsContext';
import { BatchOperationsSubmit } from './components/BatchOperationsSubmit';
import { OperationContainer } from './components/OperationContainer';
import { OperationProps } from './types';

type Props = OperationProps;

export const OperationChangeNamespace = ({ disabled, onStart }: Props) => {
  const project = useProject();
  const selection = useTranslationsSelector((c) => c.selection);

  const [namespace, setNamespace] = useState<string>('');

  const batchLoadable = useApiMutation({
    url: '/v2/projects/{projectId}/start-batch-job/set-keys-namespace',
    method: 'post',
  });

  function handleSubmit() {
    batchLoadable.mutate(
      {
        path: { projectId: project.id },
        content: {
          'application/json': {
            keyIds: selection,
            namespace: namespace || '',
          },
        },
      },
      {
        onSuccess(data) {
          onStart(data);
        },
      }
    );
  }

  return (
    <OperationContainer>
      <NamespaceSelector
        value={namespace}
        onChange={(value) => setNamespace(value || '')}
        SearchSelectProps={{ SelectProps: { sx: { minWidth: 200 } } }}
      />
      <BatchOperationsSubmit
        loading={batchLoadable.isLoading}
        disabled={disabled || namespace === undefined}
        onClick={handleSubmit}
      />
    </OperationContainer>
  );
};
