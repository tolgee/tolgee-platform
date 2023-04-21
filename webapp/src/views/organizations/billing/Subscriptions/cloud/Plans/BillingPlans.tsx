import { styled } from '@mui/material';

import { components } from 'tg.service/billingApiSchema.generated';
import { BillingPeriodType } from './PeriodSwitch';
import { CloudPlan } from './CloudPlan';

type PlanModel = components['schemas']['CloudPlanModel'];
type ActivePlanModel = components['schemas']['ActiveCloudPlanModel'];

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
              <CloudPlan
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
