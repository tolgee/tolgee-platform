import { confirmation } from 'tg.hooks/confirmation';
import { T } from '@tolgee/react';
import { useBillingApiMutation } from 'tg.service/http/useQueryApi';
import { useGlobalActions } from 'tg.globalContext/GlobalContext';
import { useMessage } from 'tg.hooks/useSuccessMessage';
import { useOrganization } from 'tg.views/organizations/useOrganization';

export const useCancelCloudSubscription = () => {
  const { refetchInitialData } = useGlobalActions();
  const messaging = useMessage();

  const organization = useOrganization();

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
          refetchInitialData();
          messaging.success(
            <T keyName="billing_subscription_will_be_cancelled_success_message" />
          );
        },
      }
    );
  };

  const doCancel = () => {
    confirmation({
      title: <T keyName="billing_cancel_dialog_title" />,
      message: <T keyName="billing_cancel_dialog_message" />,
      onConfirm: onCancel,
    });
  };

  return {
    doCancel,
    cancelMutation,
  };
};
