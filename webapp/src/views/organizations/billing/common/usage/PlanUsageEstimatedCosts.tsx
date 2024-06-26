import { FC } from 'react';
import { EstimatedCosts, EstimatedCostsProps } from './EstimatedCosts';
import { Box } from '@mui/material';

export const PlanUsageEstimatedCosts: FC<EstimatedCostsProps> = (props) => {
  return (
    <Box>
      <EstimatedCosts {...props} />
    </Box>
  );
};
