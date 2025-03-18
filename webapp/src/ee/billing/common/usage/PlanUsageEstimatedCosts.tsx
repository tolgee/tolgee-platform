import { FC } from 'react';
import { ExpectedUsage, EstimatedCostsProps } from './ExpectedUsage';
import { Box } from '@mui/material';

export const PlanUsageEstimatedCosts: FC<EstimatedCostsProps> = (props) => {
  return (
    <Box>
      <ExpectedUsage {...props} />
    </Box>
  );
};
