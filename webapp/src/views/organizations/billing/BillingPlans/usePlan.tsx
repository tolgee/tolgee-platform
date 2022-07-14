import { T } from '@tolgee/react';

import { components } from 'tg.service/billingApiSchema.generated';
import { useBillingApiMutation } from 'tg.service/http/useQueryApi';
import { useOrganization } from 'tg.views/organizations/useOrganization';
import { useMessage } from 'tg.hooks/useSuccessMessage';

type Period = components['schemas']['SubscribeRequest']['period'];

type Props = {
  planId: number;
  period: Period;
};

export const usePlan = ({ planId, period }: Props) => {
  const organization = useOrganization();
  const messaging = useMessage();

  const onPrepareUpgrade = () => {
    prepareUpgradeMutation.mutate({
      path: {
        organizationId: organization!.id,
      },
      content: {
        'application/json': {
          planId,
          period,
        },
      },
    });
  };

  const prepareUpgradeMutation = useBillingApiMutation({
    url: '/v2/organizations/{organizationId}/billing/prepare-update-subscription',
    method: 'put',
    invalidatePrefix: '/',
  });

  const onSubscribe = () => {
    subscribeMutation.mutate({
      path: {
        organizationId: organization!.id,
      },
      content: {
        'application/json': {
          planId,
          period,
        },
      },
    });
  };

  const subscribeMutation = useBillingApiMutation({
    url: '/v2/organizations/{organizationId}/billing/subscribe',
    method: 'post',
    invalidatePrefix: '/',
    options: {
      onSuccess: (data) => {
        window.location.href = data.url;
      },
      onError: (data) => {
        if (data.code === 'organization_already_subscribed') {
          messaging.error(
            <T keyName="billing_organization_already_subscribed" />
          );
        }
      },
    },
  });

  const cancelMutation = useBillingApiMutation({
    url: '/v2/organizations/{organizationId}/billing/cancel-subscription',
    method: 'put',
    invalidatePrefix: '/',
  });

  const onCancel = () => {
    cancelMutation.mutate(
      { path: { organizationId: organization!.id } },
      {
        onSuccess() {
          messaging.success(
            <T keyName="billing_plan_cancel_success_message" />
          );
        },
      }
    );
  };

  return {
    onPrepareUpgrade,
    prepareUpgradeMutation,
    onSubscribe,
    subscribeMutation,
    onCancel,
    cancelMutation,
  };
};
