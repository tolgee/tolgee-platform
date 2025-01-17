import { FC } from 'react';
import { Alert } from '@mui/material';
import { components } from 'tg.service/billingApiSchema.generated';
import { ProgressData } from '../../component/utils';
import { TrialAlertContent } from './TrialAlertContent';

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
