import { useEffect, useState } from 'react';
import { useLocation } from 'react-router-dom';
import { useTranslate } from '@tolgee/react';
import { Box, styled, Typography } from '@mui/material';

import { useOrganization } from '../../../useOrganization';
import {
  useBillingApiMutation,
  useBillingApiQuery,
} from 'tg.service/http/useQueryApi';
import { EmptyListMessage } from 'tg.component/common/EmptyListMessage';
import { SelfHostedEePlan } from './SelfHostedEePlan';
import { SelfHostedEeActiveSubscription } from './SelfHostedEeActiveSubscription';
import { BillingPeriodType } from '../cloud/Plans/PeriodSwitch';
import { useGlobalLoading } from 'tg.component/GlobalLoading';
import { components } from 'tg.service/billingApiSchema.generated';
import { useReportEvent } from 'tg.hooks/useReportEvent';

type SelfHostedEeSubscriptionModel =
  components['schemas']['SelfHostedEeSubscriptionModel'];

const StyledShopping = styled('div')`
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(min(450px, 100%), 1fr));
  gap: 16px;
  margin: 16px 0px;
  @media (max-width: 600px) {
    grid-template-columns: 1fr;
  }
`;

const StyledActive = styled('div')`
  display: grid;
  gap: 16px;
  grid-template-columns: repeat(auto-fit, minmax(min(550px, 100%), 1fr));
`;

export const SelfHostedEeSubscriptions = () => {
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
    }
  }, [isSuccess]);

  const activeSubscriptions =
    activeSubscriptionsLoadable.data?._embedded?.subscriptions;

  const loading =
    plansLoadable.isLoading ||
    activeSubscriptionsLoadable.isLoading ||
    refreshSubscriptions.isLoading;

  useGlobalLoading(loading);

  if (loading) {
    return null;
  }

  return (
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
        {!activeSubscriptions && activeSubscriptionsLoadable.isSuccess && (
          <EmptyListMessage
            height="200px"
            wrapperProps={{ py: 2 }}
            loading={activeSubscriptionsLoadable.isLoading}
          />
        )}
      </Box>

      <Typography variant="h6">
        {t('organization-billing-self-hosted-setup-new')}
      </Typography>
      <StyledShopping>
        {plansLoadable.data?._embedded?.plans?.map((plan) => (
          <SelfHostedEePlan
            key={plan.id}
            plan={plan}
            period={period}
            onChange={setPeriod}
          />
        ))}
      </StyledShopping>
    </>
  );
};
