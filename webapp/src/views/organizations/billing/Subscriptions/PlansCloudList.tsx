import { styled } from '@mui/material';

import { components } from 'tg.service/billingApiSchema.generated';
import { BillingPeriodType } from './cloud/Plans/Price/PeriodSwitch';
import { CloudPlan } from './cloud/Plans/CloudPlan';
import { planIsPeriodDependant } from './cloud/Plans/Price/PricePrimary';
import { FreePlan } from './cloud/Plans/FreePlan';
import { PlanType } from './cloud/Plans/types';
import { excludePreviousPlanFeatures } from './common/plansTools';
import { AllFromPlanFeature } from './common/AllFromPlanFeature';
import { PlanAction } from './cloud/Plans/PlanAction';

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

function isDefaultPlan(plan: PlanType) {
  return plan.free && plan.public;
}

export const PlansCloudList: React.FC<BillingPlansProps> = ({
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

  const prevPlans: PlanType[] = [];
  if (defaultPlan) {
    prevPlans.push(defaultPlan);
  }

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
        prevPlans.push(plan);

        const { filteredFeatures, previousPlanName } =
          excludePreviousPlanFeatures(prevPlans);

        const parentPlan = previousPlanName ?? defaultPlan?.name;

        return (
          <StyledPlanWrapper key={plan.id}>
            {activeSubscription && (
              <CloudPlan
                plan={plan}
                isActive={isActive(plan)}
                isEnded={isEnded(plan)}
                onPeriodChange={onPeriodChange}
                period={period}
                filteredFeatures={filteredFeatures}
                featuresMinHeight="155px"
                topFeature={
                  parentPlan && <AllFromPlanFeature planName={parentPlan} />
                }
                action={
                  <PlanAction
                    isActive={isActive(plan)}
                    isEnded={isEnded(plan)}
                    organizationHasSomeSubscription={
                      !activeSubscription.plan.free
                    }
                    period={period}
                    planId={plan.id}
                  />
                }
              />
            )}
          </StyledPlanWrapper>
        );
      })}
    </>
  );
};
