import { components } from 'tg.service/billingApiSchema.generated';
import { useBillingApiMutation } from 'tg.service/http/useQueryApi';
import { SubscriptionsCustomPlanItem } from './SubscriptionsCustomPlanItem';
import { LINKS, PARAMS } from 'tg.constants/links';
import React from 'react';
import { confirmation } from 'tg.hooks/confirmation';

type CloudPlan = components['schemas']['AdministrationCloudPlanModel'];

export function SubscriptionsCloudCustomPlanItem(props: {
  organizationAndSubscription: components['schemas']['OrganizationWithSubscriptionsModel'];
  plan: CloudPlan;
}) {
  const unassignMutation = useBillingApiMutation({
    url: '/v2/administration/organizations/{organizationId}/billing/unassign-cloud-plan/{planId}',
    method: 'put',
    invalidatePrefix: '/v2/administration',
  });

  function getOnUnassign() {
    const isActive =
      props.organizationAndSubscription.cloudSubscription?.plan.id ==
      props.plan.id;
    if (isActive) {
      return undefined;
    }

    return () => {
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
    };
  }

  return (
    <SubscriptionsCustomPlanItem
      organizationId={props.organizationAndSubscription.organization.id}
      plan={props.plan}
      editLink={`${LINKS.ADMINISTRATION_BILLING_CLOUD_PLAN_EDIT.build({
        [PARAMS.PLAN_ID]: props.plan.id,
      })}?editingForOrganizationId=${
        props.organizationAndSubscription.organization.id
      }`}
      onUnassign={getOnUnassign()}
    />
  );
}
