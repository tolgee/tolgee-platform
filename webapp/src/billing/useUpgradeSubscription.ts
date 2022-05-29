import { useOrganization } from 'tg.views/organizations/useOrganization';
import { useBillingApiMutation } from './useBillingQueryApi';

export const useUpgradeSubscription = () => {
  const organization = useOrganization();

  const onUpgrade = (planId: number) => {
    upgradeMutation.mutate({
      path: {
        organizationId: organization!.id,
      },
      content: {
        'application/json': {
          planId: planId,
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
