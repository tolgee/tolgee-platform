import { useOrganization } from 'tg.views/organizations/useOrganization';
import { useBillingApiMutation } from './useBillingQueryApi';
import { components } from 'tg.service/billingApiSchema.generated';

export const useUpgradeSubscription = (
  planId: number,
  period: components['schemas']['SubscribeRequest']['period']
) => {
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

  return {
    onUpgrade,
    upgradeMutation,
  };
};
