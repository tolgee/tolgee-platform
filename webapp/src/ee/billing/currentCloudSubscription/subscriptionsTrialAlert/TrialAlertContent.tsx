import { FC } from 'react';
import { SubscriptionsTrialAlertProps } from './SubscriptionsTrialAlert';
import { TrialAlertKeepPlanContentWithPaymentMethod } from './TrialAlertKeepPlanContentWithPaymentMethod';
import { TrialAlertKeepPlanContentWithoutPaymentMethod } from './TrialAlertKeepPlanContentWithoutPaymentMethod';
import { TrialAlertFreePlanContent } from './TrialAlertFreePlanContent';
import { TrialAlertPlanAutoRenewsContent } from './TrialAlertPlanAutoRenewsContent';

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
