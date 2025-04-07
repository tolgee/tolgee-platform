import { styled } from '@mui/material';

import { components } from 'tg.service/billingApiSchema.generated';
import { PlanType } from '../../component/Plan/types';
import { BillingPeriodType } from '../../component/Price/PeriodSwitch';
import { FreePlan } from '../../component/Plan/freePlan/FreePlan';
import { useCloudPlans } from './useCloudPlans';
import { CloudPlanItem } from './CloudPlanItem';
import { isPlanPeriodDependant } from '../../component/Plan/plansTools';

type CloudSubscriptionModel = components['schemas']['CloudSubscriptionModel'];

const StyledFreePlanWrapper = styled('div')`
  grid-column: 1 / -1;
`;

type BillingPlansProps = {
  activeSubscription: CloudSubscriptionModel;
  period: BillingPeriodType;
  onPeriodChange: (period: BillingPeriodType) => void;
};

export const PlansCloudList: React.FC<BillingPlansProps> = ({
  activeSubscription,
  period,
  onPeriodChange,
}) => {
  const { defaultPlan, plans } = useCloudPlans();

  function isActive(plan: PlanType) {
    if (activeSubscription.plan.id !== plan.id) {
      return false;
    }

    if (!isPlanPeriodDependant(plan.prices)) {
      return true;
    }

    // if trial or free, we don't care about period
    if (activeSubscription.status === 'TRIALING' || plan.free) {
      return true;
    }

    // if the period is the same, it's active
    return activeSubscription.currentBillingPeriod === period;
  }

  function isEnded(plan: PlanType) {
    return isActive(plan) && activeSubscription.cancelAtPeriodEnd;
  }

  if (!plans || !activeSubscription) {
    return null;
  }

  return (
    <>
      {defaultPlan && (
        <StyledFreePlanWrapper>
          <FreePlan
            plan={defaultPlan}
            active={isActive(defaultPlan)}
            ended={isEnded(defaultPlan)}
          />
        </StyledFreePlanWrapper>
      )}
      {plans.map((info) => {
        return (
          <CloudPlanItem
            activeSubscription={activeSubscription}
            key={info.plan.id}
            info={info}
            active={isActive(info.plan)}
            ended={isEnded(info.plan)}
            onPeriodChange={onPeriodChange}
            period={period}
          />
        );
      })}
    </>
  );
};
