import { useBillingApiMutation } from 'tg.service/http/useQueryApi';

export const useUpgradePlan = () => {
  const upgradeMutation = useBillingApiMutation({
    url: '/v2/organizations/{organizationId}/billing/update-subscription',
    method: 'put',
    invalidatePrefix: '/',
  });

  const onUpgrade = (organizationId: number, token: string) => {
    upgradeMutation.mutate({
      path: {
        organizationId: organizationId,
      },
      content: {
        'application/json': {
          token,
        },
      },
    });
  };

  return { upgradeMutation, onUpgrade };
};
