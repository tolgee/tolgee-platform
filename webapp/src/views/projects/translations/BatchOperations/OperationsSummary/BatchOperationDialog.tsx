import {
  Box,
  Button,
  Dialog,
  DialogActions,
  DialogContent,
  DialogTitle,
} from '@mui/material';
import { T, useTranslate } from '@tolgee/react';
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
import { useOperationCancel } from './useOperationCancel';
import LoadingButton from 'tg.component/common/form/LoadingButton';
import { useLoadingRegister } from 'tg.component/GlobalLoading';
import { useGlobalActions } from 'tg.globalContext/GlobalContext';

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
  const { t } = useTranslate();
  const project = useProject();
  const { incrementPlanLimitErrors, incrementSpendingLimitErrors } =
    useGlobalActions();

  const liveBatch = useProjectContext((c) =>
    c.batchOperations?.find((o) => o.id === operation.id)
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
  const isFinalizing =
    data.status === 'RUNNING' && data.progress === data.totalItems;

  const isFinished = END_STATUSES.includes(data.status);

  // supress other loadings
  useLoadingRegister(!isFinished);

  const { cancelable, handleCancel, loading } = useOperationCancel({
    operation: data,
  });

  useEffect(() => {
    if (isFinished) {
      onFinished();
    }
  }, [isFinished]);

  useEffect(() => {
    if (data.errorMessage === 'out_of_credits') {
      incrementPlanLimitErrors();
    }
    if (data.errorMessage === 'credit_spending_limit_exceeded') {
      incrementSpendingLimitErrors();
    }
  }, [data.errorMessage]);

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
          {(isFinished || data.status === 'PENDING' || isFinalizing) && (
            <Box
              data-cy="batch-operation-dialog-end-status"
              color={isFinalizing ? undefined : statusColor}
            >
              {isFinalizing
                ? t('batch-operation-dialog-finalizing')
                : statusLabel}
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
      <DialogActions sx={{ justifyContent: 'space-between' }}>
        {cancelable ? (
          <LoadingButton
            data-cy="batch-operation-dialog-cancel-job"
            loading={loading}
            onClick={handleCancel}
          >
            {t('batch_operations_dialog_cancel_job')}
          </LoadingButton>
        ) : (
          <div />
        )}

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
