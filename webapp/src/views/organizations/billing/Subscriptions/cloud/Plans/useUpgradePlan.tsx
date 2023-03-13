import { T } from '@tolgee/react';

import { useMessage } from 'tg.hooks/useSuccessMessage';
import { useBillingApiMutation } from 'tg.service/http/useQueryApi';

export const useUpgradePlan = () => {
  const messaging = useMessage();

  const upgradeMutation = useBillingApiMutation({
    url: '/v2/organizations/{organizationId}/billing/update-subscription',
    method: 'put',
    invalidatePrefix: '/',
  });

  const onUpgrade = (organizationId: number, token: string) => {
    upgradeMutation.mutate(
      {
        path: {
          organizationId: organizationId,
        },
        content: {
          'application/json': {
            token,
          },
        },
      },
      {
        onSuccess() {
          messaging.success(
            <T keyName="billing_plan_update_success_message" />
          );
        },
      }
    );
  };

  return { upgradeMutation, onUpgrade };
};
