import React, { FC } from 'react';
import { Box, Chip, IconButton, Tooltip, Typography } from '@mui/material';
import { T } from '@tolgee/react';
import { Link } from 'react-router-dom';
import { Edit02, XCircle } from '@untitled-ui/icons-react';
import { components } from 'tg.service/billingApiSchema.generated';

type SubscriptionsCustomPlanItemProps = {
  plan: {
    id: number;
    name: string;
    prices: components['schemas']['PlanPricesModel'];
    exclusiveForOrganizationId?: number;
  };
  organizationId: number;
  editLink: string;
  onUnassign?: () => void;
};

export const SubscriptionsCustomPlanItem: FC<
  SubscriptionsCustomPlanItemProps
> = ({ plan, organizationId, editLink, onUnassign }) => {
  return (
    <Box
      display="flex"
      data-cy="administration-subscriptions-custom-plans-item"
    >
      <Typography variant={'body1'}>{plan.name}</Typography>
      {plan.exclusiveForOrganizationId == organizationId && (
        <Tooltip title={<T keyName="admin_billing_cloud_plan_excliusive" />}>
          <Chip
            sx={{ ml: 0.5 }}
            color="primary"
            label="E"
            size="small"
            data-cy="administration-billing-exclusive-plan-chip"
          />
        </Tooltip>
      )}
      <Typography variant={'body1'} sx={{ ml: 2 }}>
        €{plan.prices.subscriptionMonthly} / €{plan.prices.subscriptionYearly}
      </Typography>

      <IconButton
        data-cy={'administration-billing-edit-custom-plan-button'}
        sx={{ p: '4px', ml: 1 }}
        component={Link}
        to={editLink}
      >
        <Edit02 height="20px" width="20px" />
      </IconButton>
      {onUnassign && (
        <IconButton
          data-cy={'administration-billing-unassign-custom-plan-button'}
          sx={{ p: '4px', ml: 1 }}
          onClick={onUnassign}
        >
          <XCircle height="20px" width="20px" />
        </IconButton>
      )}
    </Box>
  );
};
