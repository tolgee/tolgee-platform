import { Box, Chip, IconButton, Tooltip, Typography } from '@mui/material';
import { T } from '@tolgee/react';
import React, { FC } from 'react';
import { components } from 'tg.service/billingApiSchema.generated';
import { useBillingApiQuery } from 'tg.service/http/useQueryApi';
import { PaginatedHateoasList } from 'tg.component/common/list/PaginatedHateoasList';
import { Edit02 } from '@untitled-ui/icons-react';
import { Link } from 'react-router-dom';
import { LINKS, PARAMS } from 'tg.constants/links';

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
          <Box display="flex">
            <Typography variant={'body1'}>{plan.name}</Typography>
            {plan.exclusiveForOrganizationId == item.organization.id && (
              <Tooltip
                title={<T keyName="admin_billing_cloud_plan_excliusive" />}
              >
                <Chip sx={{ ml: 0.5 }} color="primary" label="E" size="small" />
              </Tooltip>
            )}
            <Typography variant={'body1'} sx={{ ml: 2 }}>
              €{plan.prices.subscriptionMonthly} / €
              {plan.prices.subscriptionYearly}
            </Typography>

            <IconButton
              sx={{ p: '4px', ml: 1 }}
              component={Link}
              to={`${LINKS.ADMINISTRATION_BILLING_CLOUD_PLAN_EDIT.build({
                [PARAMS.PLAN_ID]: plan.id,
              })}?editingForOrganizationId=${item.organization.id}`}
            >
              <Edit02 height="20px" width="20px" />
            </IconButton>
          </Box>
        )}
        loadable={plansLoadable}
      />
    </Box>
  );
};
