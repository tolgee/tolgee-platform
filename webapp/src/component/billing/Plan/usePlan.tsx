import { T } from '@tolgee/react';

import { useBillingApiMutation } from 'tg.service/http/useQueryApi';
import { useMessage } from 'tg.hooks/useSuccessMessage';
import { useOrganization } from 'tg.views/organizations/useOrganization';
import { BillingPeriodType } from '../Price/PeriodSwitch';
import { useGlobalActions } from 'tg.globalContext/GlobalContext';

type Props = {
  planId: number;
  period: BillingPeriodType;
};

export const usePlan = ({ planId, period }: Props) => {
  const organization = useOrganization();
  const messaging = useMessage();
  const { refetchInitialData } = useGlobalActions();

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
    subscribeMutation.mutate(
      {
        path: {
          organizationId: organization!.id,
        },
        content: {
          'application/json': {
            planId,
            period,
          },
        },
      },
      {
        onSuccess: (data) => {
          window.location.href = data.url;
        },
        onError: (data) => {
          if (data.code === 'organization_already_subscribed') {
            messaging.error(
              <T keyName="billing_organization_already_subscribed" />
            );
          } else {
            data.handleError?.();
          }
        },
      }
    );
  };

  const subscribeMutation = useBillingApiMutation({
    url: '/v2/organizations/{organizationId}/billing/subscribe',
    method: 'post',
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
          refetchInitialData();
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
