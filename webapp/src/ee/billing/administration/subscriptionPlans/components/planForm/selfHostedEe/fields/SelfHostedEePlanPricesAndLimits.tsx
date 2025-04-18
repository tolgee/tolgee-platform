import { FC } from 'react';
import { PlanPricesAndLimits } from '../../genericFields/PlanPricesAndLimits';
import { SelfHostedEePlanFormData } from '../../cloud/types';
import { usePlanFormValues } from '../../cloud/usePlanFormValues';

export const SelfHostedEePlanPricesAndLimits: FC<{
  parentName?: string;
  canEditPrices: boolean;
}> = ({ parentName, canEditPrices }) => {
  const { values } = usePlanFormValues<SelfHostedEePlanFormData>(parentName);

  return (
    <PlanPricesAndLimits
      parentName={parentName}
      canEditPrices={canEditPrices}
      isPayAsYouGo={values.isPayAsYouGo}
      metricType={'KEYS_SEATS'}
    />
  );
};
