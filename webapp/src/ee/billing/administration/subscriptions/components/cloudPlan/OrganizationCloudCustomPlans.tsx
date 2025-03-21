import { Box, IconButton, Tooltip, Typography } from '@mui/material';
import { T, useTranslate } from '@tolgee/react';
import React, { FC } from 'react';
import { components } from 'tg.service/billingApiSchema.generated';
import { useBillingApiQuery } from 'tg.service/http/useQueryApi';
import { CloudCustomPlanItem } from './CloudCustomPlanItem';
import { Plus } from '@untitled-ui/icons-react';
import { PaginatedHateoasList } from 'tg.component/common/list/PaginatedHateoasList';
import { SubscriptionsAddPlanButton } from './SubscriptionsAddPlanButton';

type OrganizationCloudCustomPlansProps = {
  item: components['schemas']['OrganizationWithSubscriptionsModel'];
  onOpenAddPlanDialog: () => void;
};

export const OrganizationCloudCustomPlans: FC<
  OrganizationCloudCustomPlansProps
> = ({ item, onOpenAddPlanDialog }) => {
  const plansLoadable = useBillingApiQuery({
    url: '/v2/administration/billing/cloud-plans',
    method: 'get',
    query: {
      filterAssignableToOrganization: item.organization.id,
      filterPublic: false,
    },
  });

  return (
    <Box sx={{ mt: 3 }}>
      <Box
        sx={{
          display: 'flex',
          alignItems: 'center',
        }}
      >
        <Typography variant="h4" sx={{ fontSize: 16, fontWeight: 'bold' }}>
          <T keyName="admin_billing_organization_custom_plans" />
        </Typography>
        <SubscriptionsAddPlanButton onClick={onOpenAddPlanDialog} />
      </Box>
      <PaginatedHateoasList
        emptyPlaceholder={
          <Box sx={{ mt: 1, fontStyle: 'italic', opacity: 0.8 }}>
            <T keyName="admin_billing_no_custom_plans" />
          </Box>
        }
        wrapperComponent={Box}
        wrapperComponentProps={{ sx: { mt: 1 } }}
        renderItem={(plan) => (
          <CloudCustomPlanItem
            organizationId={item.organization.id}
            plan={plan}
          />
        )}
        loadable={plansLoadable}
      />
    </Box>
  );
};
