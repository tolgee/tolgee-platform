import { Box, Typography } from '@mui/material';
import { T } from '@tolgee/react';
import React, { FC } from 'react';
import { components } from 'tg.service/billingApiSchema.generated';
import { useBillingApiQuery } from 'tg.service/http/useQueryApi';
import { PaginatedHateoasList } from 'tg.component/common/list/PaginatedHateoasList';
import { CloudCustomPlanItem } from './CloudCustomPlanItem';

type OrganizationCloudCustomPlansProps = {
  item: components['schemas']['OrganizationWithSubscriptionsModel'];
};

export const OrganizationCloudCustomPlans: FC<
  OrganizationCloudCustomPlansProps
> = ({ item }) => {
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
      <Typography variant="h4" sx={{ fontSize: 16, fontWeight: 'bold' }}>
        <T keyName="admin_billing_organization_custom_plans" />
      </Typography>
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
