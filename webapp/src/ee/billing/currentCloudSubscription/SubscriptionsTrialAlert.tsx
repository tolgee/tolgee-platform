import { FC } from 'react';
import { Alert, Box, Button, Link } from '@mui/material';
import { T } from '@tolgee/react';
import { components } from 'tg.service/billingApiSchema.generated';
import { useDateFormatter } from 'tg.hooks/useLocale';
import { useCancelCloudSubscription } from '../Subscriptions/cloud/useCancelCloudSubscription';
import LoadingButton from 'tg.component/common/form/LoadingButton';
import { useRestoreCloudSubscription } from '../Subscriptions/cloud/useRestoreCloudSubscription';
import { useGoToStripeCustomerPortal } from '../Subscriptions/cloud/useGoToStripeCustomerPortal';

type SubscriptionsTrialAlertProps = {
  subscription: components['schemas']['CloudSubscriptionModel'];
};

export const SubscriptionsTrialAlert: FC<SubscriptionsTrialAlertProps> = ({
  subscription,
}) => {
  if (subscription.status != 'TRIALING') {
    return null;
  }

  return (
    <>
      <Alert severity="info" sx={{ mb: 2 }} data-cy="subscriptions-trial-alert">
        <TrialAlertContent subscription={subscription} />
      </Alert>
    </>
  );
};

export const TrialAlertContent: FC<SubscriptionsTrialAlertProps> = ({
  subscription,
}) => {
  if (subscription.plan.free) {
    return <TrialAlertFreePlanContent subscription={subscription} />;
  }

  if (subscription.trialRenew) {
    return <TrialAlertPlanAutoRenewsContent subscription={subscription} />;
  }

  if (subscription.hasPaymentMethod) {
    return (
      <TrialAlertKeepPlanContentWithPaymentMethod subscription={subscription} />
    );
  }

  return (
    <TrialAlertKeepPlanContentWithoutPaymentMethod
      subscription={subscription}
    />
  );
};

export const TrialAlertKeepPlanContentWithPaymentMethod: FC<
  SubscriptionsTrialAlertProps
> = ({ subscription }) => {
  const formatDate = useDateFormatter();

  const { restoreMutation, onRestore } = useRestoreCloudSubscription();

  return (
    <>
      <T
        keyName="billing-subscription-trial-cancelled-alert"
        params={{ trialEnd: formatDate(subscription.trialEnd), b: <b /> }}
      />
      <Box sx={{ mt: 2 }}>
        <LoadingButton
          color="primary"
          variant="contained"
          loading={restoreMutation.isLoading}
          onClick={onRestore}
          data-cy={'billing-subscription-trial-alert-keep-button'}
        >
          <T keyName="billing-subscription-trial-alert-keep-button" />
        </LoadingButton>
      </Box>
    </>
  );
};

export const TrialAlertKeepPlanContentWithoutPaymentMethod: FC<
  SubscriptionsTrialAlertProps
> = ({ subscription }) => {
  const formatDate = useDateFormatter();

  const goToStripeCustomerPortal = useGoToStripeCustomerPortal();

  return (
    <>
      <T
        keyName="billing-subscription-not-to-renew-alert"
        params={{ trialEnd: formatDate(subscription.trialEnd), b: <b /> }}
      />
      <Box mt={2}>
        <Button
          data-cy="billing-trial-setup-payment-method-button"
          onClick={goToStripeCustomerPortal}
          variant="contained"
          color="primary"
        >
          <T keyName="billing-trial-setup-payment-method-button" />{' '}
        </Button>
      </Box>
    </>
  );
};

export const TrialAlertFreePlanContent: FC<SubscriptionsTrialAlertProps> = ({
  subscription,
}) => {
  const formatDate = useDateFormatter();

  return (
    <>
      <T
        keyName="billing-subscription-free-trial-alert"
        params={{ trialEnd: formatDate(subscription.trialEnd), b: <b /> }}
      />
    </>
  );
};

export const TrialAlertPlanAutoRenewsContent: FC<
  SubscriptionsTrialAlertProps
> = ({ subscription }) => {
  const formatDate = useDateFormatter();

  const { doCancel } = useCancelCloudSubscription();

  return (
    <>
      <T
        keyName="billing-subscription-auto-renews-alert"
        params={{ trialEnd: formatDate(subscription.trialEnd), b: <b /> }}
      />
      <Box mt={2}>
        <T
          keyName="billing-subscription-auto-renews-alert-cancel-message"
          params={{
            link: (
              <Link
                data-cy="billing-subscription-auto-renews-alert-cancel-button"
                onClick={doCancel}
                sx={{ cursor: 'pointer' }}
              />
            ),
          }}
        />
      </Box>
    </>
  );
};
