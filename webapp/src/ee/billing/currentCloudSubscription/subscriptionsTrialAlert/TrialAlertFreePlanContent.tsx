import { FC } from 'react';
import { SubscriptionsTrialAlertProps } from './SubscriptionsTrialAlert';
import { useDateFormatter } from 'tg.hooks/useLocale';
import { T } from '@tolgee/react';

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
