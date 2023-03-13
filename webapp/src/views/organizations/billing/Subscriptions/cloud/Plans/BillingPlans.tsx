import { styled } from '@mui/material';

import { components } from 'tg.service/billingApiSchema.generated';
import { BillingPeriodType } from './PeriodSwitch';
import { Plan } from './Plan';

type PlanModel = components['schemas']['PlanModel'];
type ActivePlanModel = components['schemas']['ActivePlanModel'];

const StyledPlanWrapper = styled('div')`
  display: grid;
`;

type BillingPlansProps = {
  plans: PlanModel[];
  activePlan: ActivePlanModel;
  period: BillingPeriodType;
  onPeriodChange: (period: BillingPeriodType) => void;
};

export const BillingPlans: React.FC<BillingPlansProps> = ({
  plans,
  activePlan,
  period,
  onPeriodChange,
}) => {
  return (
    <>
      {plans.map((plan) => {
        const planPeriod = plan.free ? undefined : period;
        const isActive =
          activePlan.id === plan.id &&
          (activePlan.currentBillingPeriod === planPeriod ||
            planPeriod === undefined);
        const isEnded = isActive && activePlan.cancelAtPeriodEnd;

        return (
          <StyledPlanWrapper key={plan.id}>
            {activePlan && (
              <Plan
                plan={plan}
                isActive={isActive}
                isEnded={isEnded}
                isOrganizationSubscribed={!activePlan.free}
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
