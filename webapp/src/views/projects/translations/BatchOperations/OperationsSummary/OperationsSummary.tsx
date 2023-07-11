import { ExpandMore, PlayCircleOutline } from '@mui/icons-material';
import { Box, Popper, styled, Tooltip } from '@mui/material';

import { useProjectContext } from 'tg.hooks/ProjectContext';
import { BatchIndicator } from './BatchIndicator';
import { OperationsList } from './OperationsList';

const StyledPopper = styled(Popper)`
  .MuiTooltip-tooltip {
    max-width: none;
  }
`;

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
    <Tooltip
      title={<OperationsList data={batchOperations} />}
      PopperComponent={StyledPopper}
    >
      <Box display="flex" gap={1} alignItems="center">
        <Box sx={{ mt: 0.75 }}>
          <PlayCircleOutline fontSize="small" />
        </Box>
        <Box>
          <BatchIndicator key={relevantTask.id} data={relevantTask} />
        </Box>
        <Box sx={{ mt: 0.75 }}>
          <ExpandMore fontSize="small" />
        </Box>
      </Box>
    </Tooltip>
  );
};
