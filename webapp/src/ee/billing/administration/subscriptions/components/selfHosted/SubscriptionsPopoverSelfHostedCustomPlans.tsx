import React, { FC } from 'react';
import { components } from 'tg.service/billingApiSchema.generated';
import { useBillingApiQuery } from 'tg.service/http/useQueryApi';
import { SubscriptionsPopoverCustomPlans } from '../generic/SubscriptionsPopoverCustomPlans';
import { SubscriptionsCustomPlanItem } from '../cloudPlan/SubscriptionsCustomPlanItem';
import { LINKS, PARAMS } from 'tg.constants/links';

type OrganizationCloudCustomPlansProps = {
  item: components['schemas']['OrganizationWithSubscriptionsModel'];
};

export const SubscriptionsPopoverSelfHostedCustomPlans: FC<
  OrganizationCloudCustomPlansProps
> = ({ item }) => {
  const getLoadable = () =>
    useBillingApiQuery({
      url: '/v2/administration/billing/self-hosted-ee-plans',
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
          editLink={`${LINKS.ADMINISTRATION_BILLING_EE_PLAN_EDIT.build({
            [PARAMS.PLAN_ID]: plan.id,
          })}?editingForOrganizationId=${item.organization.id}`}
        />
      )}
    />
  );
};
