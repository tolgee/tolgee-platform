import { FC } from 'react';
import { SubscriptionsTrialAlertProps } from './SubscriptionsTrialAlert';
import { TrialAlertFreePlanContent } from './TrialAlertFreePlanContent';

export const TrialAlertContent: FC<SubscriptionsTrialAlertProps> = (props) => {
  return <TrialAlertFreePlanContent {...props} />;
};
