import { Box, Chip, styled, Typography } from '@mui/material';
import { PricePrimary } from 'tg.ee.module/billing/component/Price/PricePrimary';
import React from 'react';
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
  overflow-wrap: anywhere;
`;

type Props = {
  plan: CloudPlan | SelfHostedEePlanAdministrationModel;
  label: string;
};

export const PlanMigrationPlanPriceDetail = ({ plan, label }: Props) => {
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
          <Typography variant="caption">{label}</Typography>
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
