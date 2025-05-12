import { components } from 'tg.service/billingApiSchema.generated';
import { useBillingApiMutation } from 'tg.service/http/useQueryApi';
import { LINKS, PARAMS } from 'tg.constants/links';
import React from 'react';
import { confirmation } from 'tg.hooks/confirmation';
import { SelfHostedEePlanModel } from '../../../../Subscriptions/selfHosted/PlansSelfHostedList';
import { SubscriptionsCustomPlanItem } from '../cloudPlan/SubscriptionsCustomPlanItem';

export function SubscriptionsSelfHostedCustomPlanItem(props: {
  organizationAndSubscription: components['schemas']['OrganizationWithSubscriptionsModel'];
  plan: SelfHostedEePlanModel;
}) {
  const unassignMutation = useBillingApiMutation({
    url: '/v2/administration/organizations/{organizationId}/billing/unassign-self-hosted-plan/{planId}',
    method: 'put',
    invalidatePrefix: '/v2/administration',
  });

  function onUnassign() {
    confirmation({
      onConfirm: () => {
        unassignMutation.mutate({
          path: {
            organizationId: props.organizationAndSubscription.organization.id,
            planId: props.plan.id,
          },
        });
      },
    });
  }

  return (
    <SubscriptionsCustomPlanItem
      organizationId={props.organizationAndSubscription.organization.id}
      plan={props.plan}
      editLink={`${LINKS.ADMINISTRATION_BILLING_EE_PLAN_EDIT.build({
        [PARAMS.PLAN_ID]: props.plan.id,
      })}?editingForOrganizationId=${
        props.organizationAndSubscription.organization.id
      }`}
      onUnassign={onUnassign}
    />
  );
}
