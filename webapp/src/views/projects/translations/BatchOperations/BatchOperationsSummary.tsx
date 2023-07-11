import { ExpandMore } from '@mui/icons-material';
import { Box } from '@mui/material';
import { useProjectContext } from 'tg.hooks/ProjectContext';
import { BatchProgress } from './BatchProgress';

export const BatchOperationsSummary = () => {
  const batchOperations = useProjectContext((c) => c.batchOperations);
  const running = batchOperations?.find((o) => o.status === 'RUNNING');
  const pending = batchOperations?.find((o) => o.status === 'PENDING');
  const failed = batchOperations?.find((o) => o.status === 'FAILED');
  const cancelled = batchOperations?.find((o) => o.status === 'CANCELLED');
  const success = batchOperations?.find((o) => o.status === 'SUCCESS');

  const relevantTask = running || pending || failed || cancelled || success;

  if (!relevantTask) {
    return null;
  }

  return (
    <Box display="flex" gap={1} alignItems="center">
      <Box>
        <BatchProgress
          max={relevantTask.totalItems}
          progress={relevantTask.progress}
        />
      </Box>
      <Box
        role="button"
        sx={{ cursor: 'pointer', mt: 0.5, alignItems: 'center' }}
      >
        <ExpandMore fontSize="small" />
      </Box>
    </Box>
  );
};
