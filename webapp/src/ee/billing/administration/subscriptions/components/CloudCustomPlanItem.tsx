import React, { FC } from 'react';
import { Box, Chip, IconButton, Tooltip, Typography } from '@mui/material';
import { T } from '@tolgee/react';
import { Link } from 'react-router-dom';
import { LINKS, PARAMS } from 'tg.constants/links';
import { Edit02 } from '@untitled-ui/icons-react';
import { components } from 'tg.service/billingApiSchema.generated';

type CloudCustomPlanItemProps = {
  plan: components['schemas']['AdministrationCloudPlanModel'];
  organizationId: number;
};

export const CloudCustomPlanItem: FC<CloudCustomPlanItemProps> = ({
  plan,
  organizationId,
}) => {
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
        to={`${LINKS.ADMINISTRATION_BILLING_CLOUD_PLAN_EDIT.build({
          [PARAMS.PLAN_ID]: plan.id,
        })}?editingForOrganizationId=${organizationId}`}
      >
        <Edit02 height="20px" width="20px" />
      </IconButton>
    </Box>
  );
};
