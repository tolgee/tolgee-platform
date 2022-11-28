import { FC } from 'react';
import { PlanEstimatedCostsArea } from '../../Subscriptions/common/Plan';
import { EstimatedCosts, EstimatedCostsProps } from './EstimatedCosts';

export const PlanUsageEstimatedCosts: FC<EstimatedCostsProps> = (props) => {
  return (
    <PlanEstimatedCostsArea>
      <EstimatedCosts {...props} />
    </PlanEstimatedCostsArea>
  );
};
