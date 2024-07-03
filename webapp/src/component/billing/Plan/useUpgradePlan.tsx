import { T } from '@tolgee/react';
import { useGlobalActions } from 'tg.globalContext/GlobalContext';

import { useMessage } from 'tg.hooks/useSuccessMessage';
import { useBillingApiMutation } from 'tg.service/http/useQueryApi';

export const useUpgradePlan = () => {
  const messaging = useMessage();
  const { refetchInitialData } = useGlobalActions();

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
          refetchInitialData();
          messaging.success(
            <T keyName="billing_plan_update_success_message" />
          );
        },
      }
    );
  };

  return { upgradeMutation, onUpgrade };
};
