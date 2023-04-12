import { useTranslate } from '@tolgee/react';
import { Box, styled, Typography } from '@mui/material';
import { useOrganization } from '../../../useOrganization';
import { useLocation } from 'react-router-dom';
import {
  useBillingApiMutation,
  useBillingApiQuery,
} from 'tg.service/http/useQueryApi';
import { useEffect } from 'react';
import { EmptyListMessage } from 'tg.component/common/EmptyListMessage';
import { SelfHostedEePlan } from './SelfHostedEePlan';
import { SelfHostedEeActiveSubscription } from './SelfHostedEeActiveSubscription';

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

  useEffect(() => {
    if (isSuccess) {
      refreshSubscriptions.mutate(
        {
          path: { organizationId: organization!.id },
        },
        {
          onSuccess: () => {
            activeSubscriptionsLoadable.refetch();
          },
        }
      );
    }
  }, [isSuccess]);

  const activeSubscriptions =
    activeSubscriptionsLoadable.data?._embedded?.subscriptions;

  return (
    <>
      <Box mb={4}>
        <Typography variant="h6" mb={2}>
          {t('organization-billing-self-hosted-active-subscriptions')}
        </Typography>
        {activeSubscriptions ? (
          <StyledActive>
            {activeSubscriptions.map((subscription) => (
              <SelfHostedEeActiveSubscription
                key={subscription.id}
                subscription={subscription}
              />
            ))}
          </StyledActive>
        ) : (
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
          <SelfHostedEePlan key={plan.id} plan={plan} />
        ))}
      </StyledShopping>
    </>
  );
};
