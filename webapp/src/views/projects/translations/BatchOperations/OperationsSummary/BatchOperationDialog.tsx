import {
  Button,
  Dialog,
  DialogActions,
  DialogContent,
  DialogTitle,
} from '@mui/material';
import { T } from '@tolgee/react';
import { useProjectContext } from 'tg.hooks/ProjectContext';

import { useProject } from 'tg.hooks/useProject';
import { useApiQuery } from 'tg.service/http/useQueryApi';
import { BatchJobModel } from '../types';
import { BatchProgress } from './BatchProgress';

type Props = {
  operation: BatchJobModel;
};

export const BatchOperationDialog = ({ operation }: Props) => {
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

  return (
    <Dialog open>
      <DialogTitle>{data.type}</DialogTitle>
      <DialogContent>
        <BatchProgress progress={data.progress} max={data.totalItems} />
        <div>
          <T
            keyName="batch_operation_progress"
            params={{
              totalItems: data.totalItems,
              progress: data.progress,
            }}
          />
        </div>
      </DialogContent>
      <DialogActions>
        <Button>
          <T keyName="batch_operations_dialog_minimize" />
        </Button>
      </DialogActions>
    </Dialog>
  );
};
