import { styled } from '@mui/material';

import { components } from 'tg.service/billingApiSchema.generated';
import { Plan } from './Plan';
import { BillingPeriodType } from './PeriodSelect';

type PlanModel = components['schemas']['PlanModel'];
type ActivePlanModel = components['schemas']['ActivePlanModel'];

const StyledPlanWrapper = styled('div')`
  display: grid;
`;

type BillingPlansProps = {
  plans: PlanModel[];
  activePlan: ActivePlanModel;
  period: BillingPeriodType;
};

export const BillingPlans: React.FC<BillingPlansProps> = ({
  plans,
  activePlan,
  period,
}) => {
  return (
    <>
      {plans.map((plan) => {
        const isActive =
          activePlan.id === plan.id &&
          activePlan.currentBillingPeriod === period;
        const isEnded = isActive && activePlan.cancelAtPeriodEnd;

        return (
          <StyledPlanWrapper key={plan.id}>
            {activePlan && (
              <Plan
                plan={plan}
                isActive={isActive}
                isEnded={isEnded}
                isOrganizationSubscribed={!activePlan.free}
                period={period}
              />
            )}
          </StyledPlanWrapper>
        );
      })}
    </>
  );
};
