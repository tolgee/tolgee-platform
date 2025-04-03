import React, { FC } from 'react';
import { components } from 'tg.service/billingApiSchema.generated';
import { useBillingApiQuery } from 'tg.service/http/useQueryApi';
import { SubscriptionsCustomPlanItem } from './SubscriptionsCustomPlanItem';
import { SubscriptionsPopoverCustomPlans } from '../generic/SubscriptionsPopoverCustomPlans';
import { LINKS, PARAMS } from 'tg.constants/links';

type OrganizationCloudCustomPlansProps = {
  item: components['schemas']['OrganizationWithSubscriptionsModel'];
};

export const SubscriptionsPopoverCloudCustomPlans: FC<
  OrganizationCloudCustomPlansProps
> = ({ item }) => {
  const getLoadable = () =>
    useBillingApiQuery({
      url: '/v2/administration/billing/cloud-plans',
      method: 'get',
      query: {
        filterAssignableToOrganization: item.organization.id,
        filterPublic: false,
      },
    });

  return (
    <SubscriptionsPopoverCustomPlans
      getLoadable={getLoadable}
      renderItem={(plan) => (
        <SubscriptionsCustomPlanItem
          organizationId={item.organization.id}
          plan={plan}
          editLink={`${LINKS.ADMINISTRATION_BILLING_CLOUD_PLAN_EDIT.build({
            [PARAMS.PLAN_ID]: plan.id,
          })}?editingForOrganizationId=${item.organization.id}`}
        />
      )}
    />
  );
};
