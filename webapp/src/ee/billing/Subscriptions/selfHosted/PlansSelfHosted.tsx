import { useEffect, useState } from 'react';
import { useLocation } from 'react-router-dom';
import { T, useTranslate } from '@tolgee/react';
import { Box, styled, Typography } from '@mui/material';

import { useOrganization } from 'tg.views/organizations/useOrganization';
import {
  useBillingApiMutation,
  useBillingApiQuery,
} from 'tg.service/http/useQueryApi';
import { BillingPeriodType } from 'tg.component/billing/Price/PeriodSwitch';
import { components } from 'tg.service/billingApiSchema.generated';
import { useReportEvent } from 'tg.hooks/useReportEvent';

import { PlansSelfHostedList } from './PlansSelfHostedList';
import { StyledBillingSectionTitle } from '../../BillingSection';
import { SelfHostedEeActiveSubscription } from './SelfHostedEeActiveSubscription';

type SelfHostedEeSubscriptionModel =
  components['schemas']['SelfHostedEeSubscriptionModel'];

const StyledShoppingGrid = styled('div')`
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(min(300px, 100%), 1fr));
  gap: 16px;
  margin: 16px 0px;
`;

const StyledActive = styled('div')`
  display: grid;
  gap: 16px;
  grid-template-columns: 1fr;
`;

export const PlansSelfHosted = () => {
  const { t } = useTranslate();

  const [period, setPeriod] = useState<BillingPeriodType>('YEARLY');
  const [newSubscription, setNewSubscription] =
    useState<SelfHostedEeSubscriptionModel>();

  const organization = useOrganization();

  const { search } = useLocation();

  const params = new URLSearchParams(search);

  const isSuccess = params.get('success') === '';

  const plansLoadable = useBillingApiQuery({
    url: `/v2/organizations/{organizationId}/billing/self-hosted-ee/plans`,
    method: 'get',
    path: {
      organizationId: organization!.id,
    },
  });

  const activeSubscriptionsLoadable = useBillingApiQuery({
    url: `/v2/organizations/{organizationId}/billing/self-hosted-ee/subscriptions`,
    method: 'get',
    path: {
      organizationId: organization!.id,
    },
  });

  const refreshSubscriptions = useBillingApiMutation({
    url: `/v2/organizations/{organizationId}/billing/self-hosted-ee/refresh-subscriptions`,
    method: 'put',
  });

  const reportEvent = useReportEvent();

  useEffect(() => {
    reportEvent('BILLING_SELF_HOSTED_EE_SUBSCRIPTIONS_VIEW');
  }, []);

  useEffect(() => {
    if (isSuccess) {
      refreshSubscriptions.mutate(
        {
          path: { organizationId: organization!.id },
        },
        {
          onSuccess: async () => {
            const result = await activeSubscriptionsLoadable.refetch();
            const subscriptions = result.data?._embedded?.subscriptions;
            if (subscriptions) {
              const newestSubscription = subscriptions.reduce((prev, curr) => {
                if (prev && prev.createdAt > curr.createdAt) {
                  return prev;
                }
                return curr;
              });
              setNewSubscription(newestSubscription);
            }
          },
        }
      );
      reportEvent('UI_SELF_HOSTED_SUBSCRIPTION_CREATED');
    }
  }, [isSuccess]);

  const activeSubscriptions =
    activeSubscriptionsLoadable.data?._embedded?.subscriptions;

  const loading =
    plansLoadable.isLoading ||
    activeSubscriptionsLoadable.isLoading ||
    refreshSubscriptions.isLoading;

  if (loading) {
    return null;
  }

  return (
    <>
      {plansLoadable.data?._embedded?.plans &&
        activeSubscriptionsLoadable.data &&
        period && (
          <>
            <Box mb={4}>
              <Typography variant="h6" mb={2}>
                {t('organization-billing-self-hosted-active-subscriptions')}
              </Typography>
              {activeSubscriptions?.length && (
                <StyledActive>
                  {activeSubscriptions?.map((subscription) => (
                    <SelfHostedEeActiveSubscription
                      key={subscription.id}
                      subscription={subscription}
                      isNew={subscription === newSubscription}
                    />
                  ))}
                </StyledActive>
              )}
              {!activeSubscriptions &&
                activeSubscriptionsLoadable.isSuccess && (
                  <T keyName="organization-billing-self-hosted-no-active-subscriptions" />
                )}
            </Box>

            <Box display="flex" justifyContent="center">
              <StyledBillingSectionTitle>
                <T keyName="organization_self_hosted_plans_title" />
              </StyledBillingSectionTitle>
            </Box>
            <StyledShoppingGrid>
              <PlansSelfHostedList
                plans={plansLoadable.data._embedded.plans}
                onPeriodChange={(period) => setPeriod(period)}
                period={period}
              />
            </StyledShoppingGrid>
          </>
        )}
    </>
  );
};
