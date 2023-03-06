import { useTranslate } from '@tolgee/react';
import {
  useBillingApiMutation,
  useBillingApiQuery,
} from 'tg.service/http/useQueryApi';
import {
  Box,
  Dialog,
  DialogContent,
  DialogTitle,
  Typography,
} from '@mui/material';
import { useOrganization } from '../useOrganization';
import { EmptyListMessage } from 'tg.component/common/EmptyListMessage';
import LoadingButton from 'tg.component/common/form/LoadingButton';
import { useLocation } from 'react-router-dom';
import { useEffect } from 'react';

export const SelfHostedEeDialog = (props: {
  open: boolean;
  onClose: () => void;
}) => {
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
    <Dialog open={props.open} onClose={props.onClose}>
      <DialogTitle>
        {t('organization-billing-self-hosted-ee-dialog-title')}
      </DialogTitle>
      <DialogContent>
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
                </Box>
              ))}
            </Box>
          ) : (
            <EmptyListMessage loading={activeSubscriptionsLoadable.isLoading} />
          )}
        </Box>

        <Typography variant="h6">
          {t('organization-billing-self-hosted-setup-new')}
        </Typography>
        <Box>
          {plansLoadable.data?._embedded?.plans?.map((plan) => (
            <LoadingButton
              key={plan.id}
              variant="outlined"
              loading={setupMutation.isLoading}
              onClick={() => {
                setupMutation.mutate({
                  path: { organizationId: organization!.id },
                  content: {
                    'application/json': { planId: plan.id },
                  },
                });
              }}
            >
              {plan.name}
            </LoadingButton>
          ))}
        </Box>
      </DialogContent>
    </Dialog>
  );
};
