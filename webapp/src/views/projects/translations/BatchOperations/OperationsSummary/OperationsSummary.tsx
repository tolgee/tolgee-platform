import { ChevronDown, PlayCircle } from '@untitled-ui/icons-react';
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
  const relevantTask =
    batchOperations?.find((o) => o.status === 'RUNNING') ||
    batchOperations?.[0];

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
          <PlayCircle width={18} height={18} />
        </Box>
        <Box>
          <BatchIndicator key={relevantTask.id} data={relevantTask} />
        </Box>
        <Box sx={{ mt: 0.75 }}>
          <ChevronDown width={18} height={18} />
        </Box>
      </Box>
    </Tooltip>
  );
};
