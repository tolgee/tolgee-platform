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
      {plans.map((plan) => (
        <StyledPlanWrapper key={plan.id}>
          {activePlan && (
            <Plan
              plan={plan}
              isActive={
                activePlan.id === plan.id &&
                activePlan.currentBillingPeriod === period
              }
              isOrganizationSubscribed={!activePlan.free}
              period={period}
            />
          )}
        </StyledPlanWrapper>
      ))}
    </>
  );
};
