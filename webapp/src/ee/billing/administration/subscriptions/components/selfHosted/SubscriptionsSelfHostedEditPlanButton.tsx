import React, { FC } from 'react';
import { components } from 'tg.service/billingApiSchema.generated';
import { LINKS, PARAMS } from 'tg.constants/links';
import { SubscriptionsEditPlanButton } from '../generic/SubscriptionsEditPlanButton';

export const SubscriptionsSelfHostedEditPlanButton: FC<{
  subscription: components['schemas']['OrganizationWithSubscriptionsModel']['selfHostedSubscriptions'][0];
  organizationId: number;
}> = ({ subscription, organizationId }) => {
  const planId = subscription.plan.id;
  if (!planId) {
    return null;
  }

  const isExclusive =
    subscription.plan.exclusiveForOrganizationId === organizationId;

  return (
    <SubscriptionsEditPlanButton
      isExclusive={isExclusive}
      link={
        LINKS.ADMINISTRATION_BILLING_EE_PLAN_EDIT.build({
          [PARAMS.PLAN_ID]: planId,
        }) + `?editingForOrganizationId=${organizationId}`
      }
    />
  );
};
