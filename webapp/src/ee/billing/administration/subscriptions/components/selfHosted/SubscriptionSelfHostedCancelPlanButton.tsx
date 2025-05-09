import React, { FC } from 'react';
import { components } from 'tg.service/billingApiSchema.generated';
import { IconButton, Tooltip } from '@mui/material';
import { XCircle } from '@untitled-ui/icons-react';
import { useBillingApiMutation } from 'tg.service/http/useQueryApi';
import { confirmation } from 'tg.hooks/confirmation';
import { useTranslate } from '@tolgee/react';

type SubscriptionCancelCurrentPlanButtonProps = {
  subscription: components['schemas']['SelfHostedEeSubscriptionAdministrationModel'];
};

export const SubscriptionSelfHostedCancelPlanButton: FC<
  SubscriptionCancelCurrentPlanButtonProps
> = ({ subscription }) => {
  const canCancel = subscription.plan?.free;

  const unassignMutation = useBillingApiMutation({
    url: '/v2/administration/billing/cancel-self-hosted-subscription/{subscriptionId}',
    method: 'put',
    invalidatePrefix: '/v2/administration',
  });

  function onCancel() {
    confirmation({
      onConfirm: () => {
        unassignMutation.mutate({
          path: {
            subscriptionId: subscription.id,
          },
        });
      },
    });
  }

  const { t } = useTranslate();

  return (
    <>
      {canCancel ? (
        <Tooltip
          title={t(
            'administration_subscription_cancel_self_hosted_plan_tooltip'
          )}
        >
          <IconButton
            onClick={onCancel}
            data-cy="admin-subscriptions-self-hosted-cancel-plan-button"
          >
            <XCircle />
          </IconButton>
        </Tooltip>
      ) : undefined}
    </>
  );
};
