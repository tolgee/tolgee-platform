import { T } from '@tolgee/react';
import { container } from 'tsyringe';

import { MessageService } from 'tg.service/MessageService';
import { components } from 'tg.service/billingApiSchema.generated';
import { useBillingApiMutation } from 'tg.service/http/useQueryApi';
import { useOrganization } from 'tg.views/organizations/useOrganization';

const messaging = container.resolve(MessageService);

type Period = components['schemas']['SubscribeRequest']['period'];

type Props = {
  planId: number;
  period: Period;
};

export const usePlan = ({ planId, period }: Props) => {
  const organization = useOrganization();

  const onUpgrade = () => {
    upgradeMutation.mutate({
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

  const upgradeMutation = useBillingApiMutation({
    url: '/v2/organizations/{organizationId}/billing/update-subscription',
    method: 'put',
    invalidatePrefix: '/v2/organizations/{organizationId}/billing/active-plan',
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
    invalidatePrefix: '/v2/organizations/{organizationId}/billing',
    options: {
      onSuccess: (data) => {
        window.location.href = data;
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
    method: 'post',
    invalidatePrefix: '/v2/organizations/{organizationId}/billing',
  });

  const onCancel = () => {
    cancelMutation.mutate({ path: { organizationId: organization!.id } });
  };

  return {
    onUpgrade,
    upgradeMutation,
    onSubscribe,
    subscribeMutation,
    onCancel,
    cancelMutation,
  };
};
