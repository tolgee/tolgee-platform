import { FC } from 'react';
import { SubscriptionsTrialAlertProps } from './SubscriptionsTrialAlert';
import { useDateFormatter } from 'tg.hooks/useLocale';
import { T } from '@tolgee/react';
import { ReachingTheLimitMessage } from './ReachingTheLimitMessage';

export const TrialAlertFreePlanContent: FC<SubscriptionsTrialAlertProps> = (
  props
) => {
  const formatDate = useDateFormatter();

  return (
    <>
      <T
        keyName="billing-subscription-free-trial-alert"
        params={{ trialEnd: formatDate(props.subscription.trialEnd), b: <b /> }}
      />
      <ReachingTheLimitMessage {...props}></ReachingTheLimitMessage>
    </>
  );
};
