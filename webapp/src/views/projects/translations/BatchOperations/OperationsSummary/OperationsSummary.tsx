import React from 'react';
import { ExpandMore, Check, PlayCircle } from '@mui/icons-material';
import { Box, Tooltip } from '@mui/material';

import { useProjectContext } from 'tg.hooks/ProjectContext';
import { BatchIndicator } from './BatchIndicator';
import { OperationsList } from './OperationsList';

export const BatchOperationsSummary = () => {
  const batchOperations = useProjectContext((c) => c.batchOperations);
  const running = batchOperations?.find((o) => o.status === 'RUNNING');
  const pending = batchOperations?.find((o) => o.status === 'PENDING');
  const failed = batchOperations?.find((o) => o.status === 'FAILED');
  const cancelled = batchOperations?.find((o) => o.status === 'CANCELLED');
  const success = batchOperations?.find((o) => o.status === 'SUCCESS');

  const relevantTask = running || pending || failed || cancelled || success;

  if (!batchOperations || !relevantTask) {
    return null;
  }

  return (
    <Tooltip title={<OperationsList data={batchOperations} />}>
      <Box display="flex" gap={1} alignItems="center">
        <Box sx={{ mt: 0.75 }}>
          {relevantTask.status === 'SUCCESS' ? (
            <Check fontSize="small" color="success" />
          ) : relevantTask.status === 'PENDING' ? (
            <PlayCircle fontSize="small" />
          ) : null}
        </Box>
        <Box>
          <BatchIndicator data={relevantTask} />
        </Box>
        <Box sx={{ mt: 0.75 }}>
          <ExpandMore fontSize="small" />
        </Box>
      </Box>
    </Tooltip>
  );
};
