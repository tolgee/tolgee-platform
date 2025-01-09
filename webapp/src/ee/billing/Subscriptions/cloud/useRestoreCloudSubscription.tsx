import { T } from '@tolgee/react';
import { useBillingApiMutation } from 'tg.service/http/useQueryApi';
import { useGlobalActions } from 'tg.globalContext/GlobalContext';
import { useMessage } from 'tg.hooks/useSuccessMessage';
import { useOrganization } from 'tg.views/organizations/useOrganization';

export const useRestoreCloudSubscription = () => {
  const { refetchInitialData } = useGlobalActions();
  const messaging = useMessage();

  const organization = useOrganization();

  const restoreMutation = useBillingApiMutation({
    url: '/v2/organizations/{organizationId}/billing/restore-cancelled-subscription',
    method: 'put',
    invalidatePrefix: '/',
  });

  const onRestore = () => {
    restoreMutation.mutate(
      { path: { organizationId: organization!.id } },
      {
        onSuccess() {
          refetchInitialData();
          messaging.success(
            <T keyName="billing_plan_restore_success_message" />
          );
        },
      }
    );
  };

  return {
    onRestore,
    restoreMutation,
  };
};
