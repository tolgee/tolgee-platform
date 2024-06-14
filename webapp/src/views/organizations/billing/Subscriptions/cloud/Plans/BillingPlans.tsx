import { styled } from '@mui/material';

import { components } from 'tg.service/billingApiSchema.generated';
import { BillingPeriodType } from './Price/PeriodSwitch';
import { CloudPlan } from './CloudPlan';
import { planIsPeriodDependant } from './Price/PricePrimary';
import { FreePlan } from './FreePlan';
import { PlanType } from './types';

type CloudSubscriptionModel = components['schemas']['CloudSubscriptionModel'];

const StyledPlanWrapper = styled('div')`
  display: grid;
`;

const StyledFreePlanWrapper = styled('div')`
  grid-column: 1 / -1;
  display: grid;
`;

type BillingPlansProps = {
  plans: PlanType[];
  activeSubscription: CloudSubscriptionModel;
  period: BillingPeriodType;
  onPeriodChange: (period: BillingPeriodType) => void;
};

function isSubset<T>(set: T[], subset: T[]): boolean {
  return subset.every((i) => set.includes(i));
}

function excludePreviousPlanFeatures(prevPaidPlans: PlanType[]) {
  const [currentPlan, ...previousPlans] = [...prevPaidPlans].reverse();
  const parentPlan = previousPlans.find((plan) =>
    isSubset(currentPlan.enabledFeatures, plan.enabledFeatures)
  );

  if (parentPlan && parentPlan.enabledFeatures.length !== 0) {
    return {
      filteredFeatures: currentPlan.enabledFeatures.filter(
        (i) => !parentPlan.enabledFeatures.includes(i)
      ),
      previousPlanName: parentPlan.name,
    };
  } else {
    return {
      filteredFeatures: currentPlan.enabledFeatures,
    };
  }
}

function isDefaultPlan(plan: PlanType) {
  return plan.free && plan.public;
}

export const BillingPlans: React.FC<BillingPlansProps> = ({
  plans,
  activeSubscription,
  period,
  onPeriodChange,
}) => {
  const defaultPlan = plans.find((p) => isDefaultPlan(p));
  const paidPlans = plans.filter((p) => p !== defaultPlan);

  // add enterprise plan
  paidPlans.push({
    id: -1,
    type: 'CONTACT_US',
    name: 'Enterprise',
    enabledFeatures: [
      'ACCOUNT_MANAGER',
      'AI_PROMPT_CUSTOMIZATION',
      'DEDICATED_SLACK_CHANNEL',
      'DEPLOYMENT_ASSISTANCE',
      'GRANULAR_PERMISSIONS',
      'MULTIPLE_CONTENT_DELIVERY_CONFIGS',
      'PREMIUM_SUPPORT',
      'PRIORITIZED_FEATURE_REQUESTS',
      'PROJECT_LEVEL_CONTENT_STORAGES',
      'STANDARD_SUPPORT',
      'WEBHOOKS',
    ],
    free: false,
    hasYearlyPrice: false,
    public: true,
  });

  const prevPaidPlans: PlanType[] = [];

  function isActive(plan: PlanType) {
    const planPeriod = plan.free ? undefined : period;
    return (
      activeSubscription.plan.id === plan.id &&
      (activeSubscription.currentBillingPeriod === planPeriod ||
        !planIsPeriodDependant(plan.prices))
    );
  }

  function isEnded(plan: PlanType) {
    return isActive(plan) && activeSubscription.cancelAtPeriodEnd;
  }

  return (
    <>
      {defaultPlan && (
        <StyledFreePlanWrapper>
          <FreePlan
            plan={defaultPlan}
            isActive={isActive(defaultPlan)}
            isEnded={isEnded(defaultPlan)}
          />
        </StyledFreePlanWrapper>
      )}
      {paidPlans.map((plan) => {
        prevPaidPlans.push(plan);

        const { filteredFeatures, previousPlanName } =
          excludePreviousPlanFeatures(prevPaidPlans);

        return (
          <StyledPlanWrapper key={plan.id}>
            {activeSubscription && (
              <CloudPlan
                plan={plan}
                isActive={isActive(plan)}
                isEnded={isEnded(plan)}
                organizationHasSomeSubscription={!activeSubscription.plan.free}
                onPeriodChange={onPeriodChange}
                period={period}
                filteredFeatures={filteredFeatures}
                allFromPlanName={previousPlanName ?? defaultPlan?.name}
              />
            )}
          </StyledPlanWrapper>
        );
      })}
    </>
  );
};
