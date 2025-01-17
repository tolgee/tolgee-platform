import { FC } from 'react';
import { SubscriptionsTrialAlertProps } from './SubscriptionsTrialAlert';
import { useDateFormatter } from 'tg.hooks/useLocale';
import { useGoToStripeCustomerPortal } from '../../Subscriptions/cloud/useGoToStripeCustomerPortal';
import { T } from '@tolgee/react';
import { Box, Button } from '@mui/material';
import { ReachingTheLimitMessage } from './ReachingTheLimitMessage';

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
