import React, { FC } from 'react';
import { components } from 'tg.service/billingApiSchema.generated';
import { IconButton, Tooltip } from '@mui/material';
import { XCircle } from '@untitled-ui/icons-react';
import { useBillingApiMutation } from 'tg.service/http/useQueryApi';
import { confirmation } from 'tg.hooks/confirmation';
import { useTranslate } from '@tolgee/react';

type SubscriptionCancelCurrentPlanButtonProps = {
  item: components['schemas']['OrganizationWithSubscriptionsModel'];
};

export const SubscriptionCloudCancelPlanButton: FC<
  SubscriptionCancelCurrentPlanButtonProps
> = ({ item }) => {
  const currentPlan = item.cloudSubscription?.plan;

  // If the plan is free and public, then it's the default free plan which canceling
  // would lead to assigning the exact same plan
  const canCancel =
    (currentPlan?.free && !currentPlan.public) ||
    item.cloudSubscription?.status === 'TRIALING';

  const unassignMutation = useBillingApiMutation({
    url: '/v2/administration/organizations/{organizationId}/billing/unassign-cloud-plan/{planId}',
    method: 'put',
    invalidatePrefix: '/v2/administration',
  });

  function onCurrentPlanCancel() {
    const planId = item.cloudSubscription?.plan?.id;

    if (planId == null) {
      return;
    }

    confirmation({
      onConfirm: () => {
        unassignMutation.mutate({
          path: {
            organizationId: item.organization.id,
            planId: planId,
          },
        });
      },
    });
  }

  const { t } = useTranslate();

  return (
    <>
      {canCancel ? (
        <Tooltip title={t('administration_subscription_cancel_plan_tooltip')}>
          <IconButton
            onClick={onCurrentPlanCancel}
            data-cy="admin-subscriptions-cloud-cancel-plan-button"
          >
            <XCircle />
          </IconButton>
        </Tooltip>
      ) : undefined}
    </>
  );
};
