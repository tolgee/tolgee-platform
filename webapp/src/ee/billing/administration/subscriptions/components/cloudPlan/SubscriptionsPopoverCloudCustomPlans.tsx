import React, { FC } from 'react';
import { components } from 'tg.service/billingApiSchema.generated';
import { useBillingApiQuery } from 'tg.service/http/useQueryApi';
import { SubscriptionsPopoverCustomPlans } from '../generic/SubscriptionsPopoverCustomPlans';
import { SubscriptionsCloudCustomPlanItem } from './SubscriptionsCloudCustomPlanItem';

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
        <SubscriptionsCloudCustomPlanItem
          organizationAndSubscription={item}
          plan={plan}
        />
      )}
    />
  );
};
