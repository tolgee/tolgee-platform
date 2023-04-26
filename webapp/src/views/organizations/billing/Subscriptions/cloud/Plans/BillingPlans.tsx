import { styled } from '@mui/material';

import { components } from 'tg.service/billingApiSchema.generated';
import { BillingPeriodType } from 'tg.component/billing/plan/PeriodSwitch';
import { CloudPlan } from './CloudPlan';

type CloudPlanModel = components['schemas']['CloudPlanModel'];
type CloudSubscriptionModel = components['schemas']['CloudSubscriptionModel'];

const StyledPlanWrapper = styled('div')`
  display: grid;
`;

type BillingPlansProps = {
  plans: CloudPlanModel[];
  activeSubscription: CloudSubscriptionModel;
  period: BillingPeriodType;
  onPeriodChange: (period: BillingPeriodType) => void;
};

export const BillingPlans: React.FC<BillingPlansProps> = ({
  plans,
  activeSubscription,
  period,
  onPeriodChange,
}) => {
  return (
    <>
      {plans.map((plan) => {
        const planPeriod = plan.free ? undefined : period;
        const isActive =
          activeSubscription.plan.id === plan.id &&
          (activeSubscription.currentBillingPeriod === planPeriod ||
            planPeriod === undefined);
        const isEnded = isActive && activeSubscription.cancelAtPeriodEnd;

        return (
          <StyledPlanWrapper key={plan.id}>
            {activeSubscription && (
              <CloudPlan
                plan={plan}
                isActive={isActive}
                isEnded={isEnded}
                isOrganizationSubscribed={!activeSubscription.plan.free}
                onPeriodChange={onPeriodChange}
                period={period}
              />
            )}
          </StyledPlanWrapper>
        );
      })}
    </>
  );
};
