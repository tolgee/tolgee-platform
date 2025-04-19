import React, { FC } from 'react';
import { components } from 'tg.service/billingApiSchema.generated';
import { LINKS, PARAMS } from 'tg.constants/links';
import { SubscriptionsEditPlanButton } from '../generic/SubscriptionsEditPlanButton';

export const SubscriptionsCloudEditPlanButton: FC<{
  item: components['schemas']['OrganizationWithSubscriptionsModel'];
}> = ({ item }) => {
  const planId = item.cloudSubscription?.plan.id;
  if (!planId) {
    return null;
  }

  return (
    <SubscriptionsEditPlanButton
      link={
        LINKS.ADMINISTRATION_BILLING_CLOUD_PLAN_EDIT.build({
          [PARAMS.PLAN_ID]: planId,
        }) + `?editingForOrganizationId=${item.organization.id}`
      }
      isExclusive={
        item.cloudSubscription?.plan.exclusiveForOrganizationId ==
        item.organization.id
      }
    />
  );
};
