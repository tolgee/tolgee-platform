import { FC } from 'react';
import { Alert, Box, Button, Link } from '@mui/material';
import { T } from '@tolgee/react';
import { components } from 'tg.service/billingApiSchema.generated';
import { useDateFormatter } from 'tg.hooks/useLocale';
import { useCancelCloudSubscription } from '../Subscriptions/cloud/useCancelCloudSubscription';
import LoadingButton from 'tg.component/common/form/LoadingButton';
import { useRestoreCloudSubscription } from '../Subscriptions/cloud/useRestoreCloudSubscription';
import { useGoToStripeCustomerPortal } from '../Subscriptions/cloud/useGoToStripeCustomerPortal';
import { ProgressData } from '../component/utils';

type SubscriptionsTrialAlertProps = {
  subscription: components['schemas']['CloudSubscriptionModel'];
  usage: ProgressData;
};

export const SubscriptionsTrialAlert: FC<SubscriptionsTrialAlertProps> = ({
  subscription,
  usage,
}) => {
  if (subscription.status != 'TRIALING') {
    return null;
  }

  return (
    <>
      <Alert severity="info" sx={{ mb: 2 }} data-cy="subscriptions-trial-alert">
        <TrialAlertContent subscription={subscription} usage={usage} />
      </Alert>
    </>
  );
};

export const TrialAlertContent: FC<SubscriptionsTrialAlertProps> = (props) => {
  if (props.subscription.plan.free) {
    return <TrialAlertFreePlanContent {...props} />;
  }

  if (props.subscription.trialRenew) {
    return <TrialAlertPlanAutoRenewsContent {...props} />;
  }

  if (props.subscription.hasPaymentMethod) {
    return <TrialAlertKeepPlanContentWithPaymentMethod {...props} />;
  }

  return <TrialAlertKeepPlanContentWithoutPaymentMethod {...props} />;
};

export const TrialAlertKeepPlanContentWithPaymentMethod: FC<
  SubscriptionsTrialAlertProps
> = (props) => {
  const formatDate = useDateFormatter();

  const { restoreMutation, onRestore } = useRestoreCloudSubscription();

  return (
    <>
      <T
        keyName="billing-subscription-trial-cancelled-alert"
        params={{ trialEnd: formatDate(props.subscription.trialEnd), b: <b /> }}
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
      <ReachingTheLimitMessage {...props}></ReachingTheLimitMessage>
    </>
  );
};

export const TrialAlertKeepPlanContentWithoutPaymentMethod: FC<
  SubscriptionsTrialAlertProps
> = (props) => {
  const formatDate = useDateFormatter();

  const goToStripeCustomerPortal = useGoToStripeCustomerPortal();

  return (
    <>
      <T
        keyName="billing-subscription-not-to-renew-alert"
        params={{ trialEnd: formatDate(props.subscription.trialEnd), b: <b /> }}
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
      <ReachingTheLimitMessage {...props}></ReachingTheLimitMessage>
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
> = (props) => {
  const formatDate = useDateFormatter();

  const { doCancel } = useCancelCloudSubscription();

  return (
    <>
      <T
        keyName="billing-subscription-auto-renews-alert"
        params={{ trialEnd: formatDate(props.subscription.trialEnd), b: <b /> }}
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
      <ReachingTheLimitMessage {...props}></ReachingTheLimitMessage>
    </>
  );
};

// TODO: Test this and that it can subscribe to the same plan when on trial
const ReachingTheLimitMessage: FC<SubscriptionsTrialAlertProps> = (props) => {
  if (props.subscription.plan.type !== 'PAY_AS_YOU_GO') {
    return null;
  }

  const runningOutOfMtCredits = props.usage.creditProgress > 0.9 || true;
  const runningOutOfTranslations = props.usage.translationsProgress > 0.9;

  if (!runningOutOfMtCredits && !runningOutOfTranslations) {
    return null;
  }

  return (
    <Box mt={2}>
      <T keyName="billing-subscription-trial-alert-reaching-the-limit-message" />
    </Box>
  );
};
