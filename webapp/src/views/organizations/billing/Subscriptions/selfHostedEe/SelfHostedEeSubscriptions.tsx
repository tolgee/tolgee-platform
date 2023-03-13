import { useTranslate } from '@tolgee/react';
import { Box, styled, Typography } from '@mui/material';
import { useOrganization } from '../../../useOrganization';
import { useLocation } from 'react-router-dom';
import {
  useBillingApiMutation,
  useBillingApiQuery,
} from 'tg.service/http/useQueryApi';
import { useEffect } from 'react';
import { CancelSelfHostedEeSubscriptionButton } from '../../CancelSelfHostedEeSubscriptionButton';
import { EmptyListMessage } from 'tg.component/common/EmptyListMessage';
import { Plan } from './Plan';

const StyledShopping = styled('div')`
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(450px, 1fr));
  gap: 16px;
  margin: 16px 0px;
  @media (max-width: 600px) {
    grid-template-columns: 1fr;
  }
`;

export const SelfHostedEeSubscriptions = () => {
  const { t } = useTranslate();

  const organization = useOrganization();

  const { search } = useLocation();

  const params = new URLSearchParams(search);

  const isSuccess = params.get('success') === '';

  const plansLoadable = useBillingApiQuery({
    url: `/v2/organizations/{organizationId}/billing/self-hosted-ee-plans`,
    method: 'get',
    path: {
      organizationId: organization!.id,
    },
  });

  const activeSubscriptionsLoadable = useBillingApiQuery({
    url: `/v2/organizations/{organizationId}/billing/self-hosted-ee-subscriptions`,
    method: 'get',
    path: {
      organizationId: organization!.id,
    },
  });

  const refreshSubscriptions = useBillingApiMutation({
    url: `/v2/organizations/{organizationId}/billing/refresh-self-hosted-ee-subscriptions`,
    method: 'put',
  });

  const setupMutation = useBillingApiMutation({
    url: '/v2/organizations/{organizationId}/billing/setup-ee',
    method: 'post',
    options: {
      onSuccess: (data) => {
        window.location.href = data.url;
      },
    },
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
        <Typography variant="h6">
          {t('organization-billing-self-hosted-active-subscriptions')}
        </Typography>
        {activeSubscriptions ? (
          <Box>
            {activeSubscriptions.map((subscription) => (
              <Box key={subscription.id}>
                {subscription.plan.name} | Subscribed:{' '}
                {new Date(subscription.createdAt).toString()}
                <CancelSelfHostedEeSubscriptionButton id={subscription.id} />
              </Box>
            ))}
          </Box>
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
          <Plan key={plan.id} planModel={plan} />
        ))}
      </StyledShopping>
    </>
  );
};
