import { FC } from 'react';
import { Alert } from '@mui/material';
import { components } from 'tg.service/billingApiSchema.generated';
import { TrialAlertContent } from './TrialAlertContent';
import { ProgressData } from '../../component/getProgressData';

export type SubscriptionsTrialAlertProps = {
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
