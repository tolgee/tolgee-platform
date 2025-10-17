import { Box, Chip, styled, Typography } from '@mui/material';
import { PricePrimary } from 'tg.ee.module/billing/component/Price/PricePrimary';
import React from 'react';
import { useTranslate } from '@tolgee/react';
import { components } from 'tg.service/billingApiSchema.generated';

type CloudPlan = components['schemas']['CloudPlanModel'];
type SelfHostedEePlanAdministrationModel =
  components['schemas']['SelfHostedEePlanModel'];

const TooltipTitle = styled('div')`
  font-weight: bold;
  font-size: 14px;
  line-height: 17px;
`;

const TooltipText = styled('div')`
  white-space: nowrap;
`;

type Props = {
  plan: CloudPlan | SelfHostedEePlanAdministrationModel;
};

export const PlanMigrationPlanPriceDetail = ({ plan }: Props) => {
  const { t } = useTranslate();
  return (
    <Chip
      sx={{
        padding: '10px',
        height: 'auto',
        '& .MuiChip-label': {
          display: 'block',
          whiteSpace: 'normal',
        },
      }}
      label={
        <Box>
          <Typography variant="caption">
            {t('administration_plan_migration_from')}
          </Typography>
          <TooltipTitle>{plan.name}</TooltipTitle>
          {plan.prices && (
            <TooltipText>
              <PricePrimary
                prices={plan.prices}
                period={'YEARLY'}
                highlightColor={''}
                sx={{
                  fontSize: 14,
                  fontWeight: 500,
                }}
                noPeriodSwitch={true}
              />
            </TooltipText>
          )}
        </Box>
      }
    />
  );
};
