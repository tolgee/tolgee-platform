import { FC } from 'react';
import { useTranslate } from '@tolgee/react';
import { Box, Tooltip } from '@mui/material';
import { PlanIncludedUsageFields } from '../../genericFields/PlanIncludedUsageFields';
import { usePlanFormValues } from '../usePlanFormValues';
import { PlanPricesFields } from '../../genericFields/PlanPricesFields';

export const CloudPlanPricesAndLimits: FC<{
  parentName?: string;
  canEditPrices: boolean;
}> = ({ parentName, canEditPrices }) => {
  const { values } = usePlanFormValues(parentName);

  return (
    <Wrapper canEditPrices={canEditPrices}>
      <PlanPricesFields parentName={parentName} />
      <PlanIncludedUsageFields
        parentName={parentName}
        metricType={values['metricType']}
      />
    </Wrapper>
  );
};

const Wrapper = ({ children, canEditPrices }) => {
  const { t } = useTranslate();

  if (!canEditPrices) {
    return (
      <Tooltip title={t('admin-billing-cannot-edit-prices-tooltip')}>
        <span>
          <Box sx={{ pointerEvents: 'none', opacity: 0.5 }}>{children}</Box>
        </span>
      </Tooltip>
    );
  }

  return <>{children}</>;
};
