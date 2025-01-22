import { FC } from 'react';
import { SubscriptionsTrialAlertProps } from './SubscriptionsTrialAlert';
import { TrialAlertKeepPlanContentWithoutPaymentMethod } from './TrialAlertKeepPlanContentWithoutPaymentMethod';
import { TrialAlertFreePlanContent } from './TrialAlertFreePlanContent';

export const TrialAlertContent: FC<SubscriptionsTrialAlertProps> = (props) => {
  if (props.subscription.plan.free) {
    return <TrialAlertFreePlanContent {...props} />;
  }

  return <TrialAlertKeepPlanContentWithoutPaymentMethod {...props} />;
};
