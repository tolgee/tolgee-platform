import { useState } from 'react';
import { ChevronRight } from '@mui/icons-material';
import { Box } from '@mui/material';

import LoadingButton from 'tg.component/common/form/LoadingButton';
import { OperationProps } from './types';
import { NamespaceSelector } from 'tg.component/NamespaceSelector/NamespaceSelector';
import { useApiMutation } from 'tg.service/http/useQueryApi';
import { useProject } from 'tg.hooks/useProject';
import { useTranslationsSelector } from '../context/TranslationsContext';

type Props = OperationProps;

export const OperationChangeNamespace = ({ disabled, onStart }: Props) => {
  const project = useProject();
  const selection = useTranslationsSelector((c) => c.selection);

  const [namespace, setNamespace] = useState<string>();

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
    <Box display="flex" gap="10px">
      <NamespaceSelector
        value={namespace}
        onChange={(value) => setNamespace(value)}
        SearchSelectProps={{ SelectProps: { sx: { minWidth: 200 } } }}
      />
      <LoadingButton
        data-cy="batch-operations-submit-button"
        loading={batchLoadable.isLoading}
        disabled={disabled || namespace === undefined}
        sx={{ minWidth: 0, minHeight: 0, width: 40, height: 40 }}
        onClick={handleSubmit}
        variant="contained"
        color="primary"
      >
        <ChevronRight />
      </LoadingButton>
    </Box>
  );
};
