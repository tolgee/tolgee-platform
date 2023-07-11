import {
  Box,
  Button,
  Dialog,
  DialogActions,
  DialogContent,
  DialogTitle,
} from '@mui/material';
import { T } from '@tolgee/react';
import { useEffect } from 'react';

import { useProject } from 'tg.hooks/useProject';
import { useApiQuery } from 'tg.service/http/useQueryApi';
import { useProjectContext } from 'tg.hooks/ProjectContext';
import { TranslatedError } from 'tg.translationTools/TranslatedError';
import { useBatchOperationStatusTranslate } from 'tg.translationTools/useBatchOperationStatusTranslate';

import { BatchJobModel } from '../types';
import { BatchProgress } from './BatchProgress';
import { END_STATUSES, useStatusColor } from './utils';
import { useBatchOperationTypeTranslate } from 'tg.translationTools/useBatchOperationTypeTranslation';

type Props = {
  operation: BatchJobModel;
  onClose: () => void;
  onFinished: () => void;
};

export const BatchOperationDialog = ({
  operation,
  onClose,
  onFinished,
}: Props) => {
  const project = useProject();

  const liveBatch = useProjectContext((c) =>
    c.batchOperations.find((o) => o.id === operation.id)
  );

  const operationLoadable = useApiQuery({
    url: '/v2/projects/{projectId}/batch-jobs/{id}',
    method: 'get',
    path: { projectId: project.id, id: operation.id },
  });

  const data = liveBatch || operationLoadable.data || operation;

  const getStatusColor = useStatusColor();

  const statusColor = getStatusColor(data.status);
  const statusLabel = useBatchOperationStatusTranslate()(data.status);
  const typeLabel = useBatchOperationTypeTranslate()(data.type);

  const isFinished = END_STATUSES.includes(data.status);

  useEffect(() => {
    if (isFinished) {
      onFinished();
    }
  }, [isFinished]);

  return (
    <Dialog open>
      <DialogTitle sx={{ width: 'min(80vw, 400px)' }}>{typeLabel}</DialogTitle>
      <DialogContent>
        <BatchProgress progress={data.progress} max={data.totalItems} />
        <Box display="flex" justifyContent="space-between" mt={1}>
          <Box>
            <T
              keyName="batch_operation_progress"
              params={{
                totalItems: data.totalItems,
                progress: data.progress,
              }}
            />
          </Box>
          {(isFinished || data.status === 'PENDING') && (
            <Box
              data-cy="batch-operation-dialog-end-status"
              color={statusColor}
            >
              {statusLabel}
            </Box>
          )}
        </Box>
        {data.errorMessage && (
          <Box
            display="flex"
            justifyContent="space-between"
            mt={1}
            color={getStatusColor('FAILED')}
          >
            <TranslatedError code={data.errorMessage} />
          </Box>
        )}
      </DialogContent>
      <DialogActions>
        {isFinished ? (
          <Button onClick={onClose} data-cy="batch-operation-dialog-ok">
            <T keyName="batch_operations_dialog_ok" />
          </Button>
        ) : (
          <Button onClick={onClose}>
            <T
              keyName="batch_operations_dialog_minimize"
              data-cy="batch-operation-dialog-minimize"
            />
          </Button>
        )}
      </DialogActions>
    </Dialog>
  );
};
