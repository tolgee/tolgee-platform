import { FunctionComponent, useEffect } from 'react';
import { T, useCurrentLanguage } from '@tolgee/react';

import { BaseOrganizationSettingsView } from 'tg.views/organizations/BaseOrganizationSettingsView';

import { useOrganization } from 'tg.views/organizations/useOrganization';
import { useLocation } from 'react-router-dom';
import { MessageService } from 'tg.service/MessageService';
import { container } from 'tsyringe';
import { Box, Button, Typography } from '@mui/material';
import {
  useBillingApiMutation,
  useBillingApiQuery,
} from './useBillingQueryApi';
import LoadingButton from 'tg.component/common/form/LoadingButton';
import { useOrganizationCreditBalance } from './useOrganizationCreditBalance';

const messaging = container.resolve(MessageService);
export const OrganizationBillingView: FunctionComponent = () => {
  const { search } = useLocation();
  const params = new URLSearchParams(search);

  const success = params.has('success');

  const getCurrentLang = useCurrentLanguage();

  const organization = useOrganization();

  const creditBalance = useOrganizationCreditBalance();

  const url = new URL(window.location.href);

  url.search = '';

  const refreshSubscription = useBillingApiMutation({
    url: `/v2/billing/refresh-subscription/{organizationId}`,
    method: `put`,
    invalidatePrefix: `/v2/billing`,
    options: {
      onSuccess: (data) => {
        creditBalance.refetch();
      },
    },
  });

  useEffect(() => {
    if (success) {
      refreshSubscription.mutate({
        path: { organizationId: organization!.id },
      });
    }
  }, [success]);

  const plansLoadable = useBillingApiQuery({
    url: '/v2/billing/plans',
    method: 'get',
  });

  const upgradeMutation = useBillingApiMutation({
    url: '/v2/billing/update-subscription',
    method: 'post',
    invalidatePrefix: '/v2/billing/active-plan',
  });

  const cancelMutation = useBillingApiMutation({
    url: '/v2/billing/cancel-subscription/{organizationId}',
    method: 'post',
    invalidatePrefix: '/v2/billing/active-plan',
  });

  const subscribeMutation = useBillingApiMutation({
    url: '/v2/billing/subscribe',
    method: 'post',
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

  const getCustomerPortalSession = useBillingApiMutation({
    url: '/v2/billing/create-customer-portal-session',
    method: 'post',
    options: {
      onSuccess: (data) => {
        window.location.href = data;
      },
    },
  });

  const activePlan = useBillingApiQuery({
    url: '/v2/billing/active-plan/{organizationId}',
    method: 'get',
    path: {
      organizationId: organization!.id,
    },
  });

  const onSubscribe = (planId: number) => {
    subscribeMutation.mutate({
      content: {
        'application/json': {
          planId: planId,
          organizationId: organization!.id,
        },
      },
    });
  };

  const onCancel = () => {
    cancelMutation.mutate({ path: { organizationId: organization!.id } });
  };

  const onUpgrade = (planId: number) => {
    upgradeMutation.mutate({
      content: {
        'application/json': {
          planId: planId,
          organizationId: organization!.id,
        },
      },
    });
  };

  return (
    <BaseOrganizationSettingsView
      title={<T>organization_billing_title</T>}
      hideChildrenOnLoading={true}
      loading={
        activePlan.isLoading ||
        plansLoadable.isLoading ||
        refreshSubscription.isLoading ||
        activePlan.isLoading
      }
    >
      <Box>
        Credit Balance: {(creditBalance.data?.creditBalance || 0) / 100} /{' '}
        {(creditBalance.data?.bucketSize || 0) / 100}
      </Box>

      {plansLoadable.data &&
        plansLoadable.data?._embedded?.plans?.map((plan) => (
          <Box key={plan.id}>
            <Box>{plan.name}</Box>
            <Box>Translation limit: {plan.translationLimit}</Box>
            <Box>
              Mt Credits included: {(plan.includedMtCredits || 0) / 100}
            </Box>
            {activePlan.data ? (
              activePlan.data.id === plan.id ? (
                <>
                  This is Active
                  <Box>
                    Period end:{' '}
                    {new Date(
                      activePlan.data.currentPeriodEnd
                    ).toLocaleDateString(getCurrentLang())}
                  </Box>
                  <Box>
                    Cancel at period end:{' '}
                    {activePlan.data.cancelAtPeriodEnd ? 'true' : 'false'}
                  </Box>
                  {activePlan.data.cancelAtPeriodEnd ? (
                    <LoadingButton
                      variant="outlined"
                      color="primary"
                      onClick={() => onUpgrade(plan.id)}
                    >
                      Subscribe
                    </LoadingButton>
                  ) : (
                    <LoadingButton
                      loading={cancelMutation.isLoading}
                      variant="outlined"
                      color="primary"
                      onClick={() => onCancel()}
                    >
                      Cancel subscription
                    </LoadingButton>
                  )}
                </>
              ) : (
                <LoadingButton
                  loading={upgradeMutation.isLoading}
                  variant="outlined"
                  color="primary"
                  onClick={() => onUpgrade(plan.id)}
                >
                  Change subscription
                </LoadingButton>
              )
            ) : (
              <LoadingButton
                loading={subscribeMutation.isLoading}
                variant="outlined"
                color="primary"
                onClick={() => onSubscribe(plan.id)}
              >
                Subscribe
              </LoadingButton>
            )}
          </Box>
        ))}
      <Typography>
        To review your invoices, update your payment method or review your
        billing info, continue to customer portal.
      </Typography>
      <Button
        variant={'outlined'}
        onClick={() =>
          getCustomerPortalSession.mutate({
            content: {
              'application/json': {
                organizationId: organization!.id,
                returnUrl: url.href,
              },
            },
          })
        }
      >
        Go to customer portal
      </Button>
    </BaseOrganizationSettingsView>
  );
};
