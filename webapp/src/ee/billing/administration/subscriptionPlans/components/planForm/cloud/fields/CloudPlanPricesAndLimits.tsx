import { FC } from 'react';
import { usePlanFormValues } from '../usePlanFormValues';
import { CloudPlanFormData } from '../types';
import { PlanPricesAndLimits } from '../../genericFields/PlanPricesAndLimits';

export const CloudPlanPricesAndLimits: FC<{
  parentName?: string;
  canEditPrices: boolean;
}> = ({ parentName, canEditPrices }) => {
  const { values } = usePlanFormValues<CloudPlanFormData>(parentName);

  return (
    <PlanPricesAndLimits
      parentName={parentName}
      canEditPrices={canEditPrices}
      isPayAsYouGo={values.type === 'PAY_AS_YOU_GO'}
      metricType={values.metricType}
    />
  );
};
