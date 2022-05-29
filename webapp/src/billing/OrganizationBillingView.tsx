import { FunctionComponent, useEffect } from 'react';
import { T, useTranslate } from '@tolgee/react';

import { BaseOrganizationSettingsView } from 'tg.views/organizations/BaseOrganizationSettingsView';

import { useOrganization } from 'tg.views/organizations/useOrganization';
import { useLocation } from 'react-router-dom';
import { Box, Button, Typography } from '@mui/material';
import {
  useBillingApiMutation,
  useBillingApiQuery,
} from './useBillingQueryApi';
import { useOrganizationCreditBalance } from './useOrganizationCreditBalance';
import { BillingPlans } from './BillingPlans';
import { MoreMtCredits } from './mtCredits/MoreMtCredits';
import { Invoices } from './invoices/Invoices';

export const OrganizationBillingView: FunctionComponent = () => {
  const { search } = useLocation();
  const params = new URLSearchParams(search);

  const success = params.has('success');

  const organization = useOrganization();

  const creditBalance = useOrganizationCreditBalance();

  const url = new URL(window.location.href);

  url.search = '';

  const refreshSubscription = useBillingApiMutation({
    url: `/v2/organizations/{organizationId}/billing/refresh-subscription`,
    method: `put`,
    invalidatePrefix: `/v2/organizations/{organizationId}/billing`,
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

  const getCustomerPortalSession = useBillingApiMutation({
    url: '/v2/organizations/{organizationId}/billing/customer-portal-session',
    method: 'post',
    options: {
      onSuccess: (data) => {
        window.location.href = data;
      },
    },
  });

  const activePlan = useBillingApiQuery({
    url: '/v2/organizations/{organizationId}/billing/active-plan',
    method: 'get',
    path: {
      organizationId: organization!.id,
    },
  });

  const t = useTranslate();

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
      windowTitle={t({ key: 'organization_billing_title', noWrap: true })}
    >
      <Box>
        Credit Balance: {(creditBalance.data?.creditBalance || 0) / 100} /{' '}
        {(creditBalance.data?.bucketSize || 0) / 100}
      </Box>

      {plansLoadable.data?._embedded?.plans && activePlan.data && (
        <BillingPlans
          plans={plansLoadable.data._embedded.plans}
          activePlan={activePlan.data}
        />
      )}

      <MoreMtCredits />

      <Typography>
        To review your invoices, update your payment method or review your
        billing info, continue to customer portal.
      </Typography>
      <Button
        variant={'outlined'}
        onClick={() =>
          getCustomerPortalSession.mutate({
            path: {
              organizationId: organization!.id,
            },
          })
        }
      >
        Go to customer portal
      </Button>

      <Invoices />
    </BaseOrganizationSettingsView>
  );
};
