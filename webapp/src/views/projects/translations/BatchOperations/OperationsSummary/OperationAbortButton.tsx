import { useState } from 'react';
import clsx from 'clsx';
import { Box, CircularProgress, styled } from '@mui/material';
import { Close } from '@mui/icons-material';

import { components } from 'tg.service/apiSchema.generated';
import { useApiMutation } from 'tg.service/http/useQueryApi';
import { useProject } from 'tg.hooks/useProject';

type BatchJobModel = components['schemas']['BatchJobModel'];

const AbortButton = styled(Box)`
  cursor: pointer;
  display: flex;
  align-items: center;
  margin: 0px -5px;
  width: 20px;
  height: 20px;

  &.disabled {
    pointer-events: none;
    color: ${({ theme }) => theme.palette.emphasis[800]};
  }
`;

type Props = {
  operation: BatchJobModel;
};

export function OperationAbortButton({ operation }: Props) {
  const project = useProject();
  const [cancelled, setCancelled] = useState(false);

  const cancelLoadable = useApiMutation({
    url: '/v2/projects/{projectId}/batch-jobs/{id}/cancel',
    method: 'put',
  });

  function handleCancel() {
    setCancelled(true);
    cancelLoadable.mutate({
      path: { projectId: project.id, id: operation.id },
    });
  }

  return (
    <AbortButton
      role="button"
      onClick={handleCancel}
      className={clsx({ disabled: cancelled })}
    >
      {!cancelled ? (
        <Close fontSize="small" color="inherit" />
      ) : (
        <CircularProgress size={18} />
      )}
    </AbortButton>
  );
}
