import { FC } from 'react';
import { useTranslate } from '@tolgee/react';
import { Box, Tooltip } from '@mui/material';
import { PlanPricesFields } from './PlanPricesFields';
import { PlanIncludedUsageFields } from './PlanIncludedUsageFields';
import { MetricType } from '../cloud/types';

export const PlanPricesAndLimits: FC<{
  parentName?: string;
  canEditPrices: boolean;
  isPayAsYouGo: boolean;
  metricType: MetricType;
}> = ({ parentName = '', canEditPrices, isPayAsYouGo, metricType }) => {
  return (
    <Wrapper canEditPrices={canEditPrices}>
      <PlanPricesFields
        parentName={parentName}
        isPayAsYouGo={isPayAsYouGo}
        metricType={metricType}
      />
      <PlanIncludedUsageFields
        parentName={parentName}
        metricType={metricType}
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
