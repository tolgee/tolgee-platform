import { styled } from '@mui/material';

import { components } from 'tg.service/billingApiSchema.generated';

import { PlanAction } from './CloudPlanAction';
import { PlanType } from '../../component/Plan/types';
import { BillingPeriodType } from '../../component/Price/PeriodSwitch';
import {
  excludePreviousPlanFeatures,
  planIsPeriodDependant,
} from '../../component/Plan/plansTools';
import { FreePlan } from '../../component/Plan/FreePlan';
import { Plan } from '../../component/Plan/Plan';
import { AllFromPlanFeature } from '../../component/Plan/AllFromPlanFeature';

type CloudSubscriptionModel = components['schemas']['CloudSubscriptionModel'];

const StyledFreePlanWrapper = styled('div')`
  grid-column: 1 / -1;
`;

type BillingPlansProps = {
  plans: PlanType[];
  activeSubscription: CloudSubscriptionModel;
  period: BillingPeriodType;
  onPeriodChange: (period: BillingPeriodType) => void;
};

export const PlansCloudList: React.FC<BillingPlansProps> = ({
  plans,
  activeSubscription,
  period,
  onPeriodChange,
}) => {
  const defaultPlan = plans.find((p) => p.free && p.public);
  const publicPlans = plans.filter((p) => p !== defaultPlan && p.public);
  const customPlans = plans.filter((p) => !p.public);

  // add enterprise plan
  publicPlans.push({
    id: -1,
    type: 'CONTACT_US',
    name: 'Enterprise',
    enabledFeatures: [
      'ACCOUNT_MANAGER',
      'AI_PROMPT_CUSTOMIZATION',
      'DEDICATED_SLACK_CHANNEL',
      'SLACK_INTEGRATION',
      'GRANULAR_PERMISSIONS',
      'MULTIPLE_CONTENT_DELIVERY_CONFIGS',
      'PREMIUM_SUPPORT',
      'PRIORITIZED_FEATURE_REQUESTS',
      'PROJECT_LEVEL_CONTENT_STORAGES',
      'STANDARD_SUPPORT',
      'WEBHOOKS',
      'TASKS',
    ],
    free: false,
    hasYearlyPrice: false,
    public: true,
    nonCommercial: false,
  });

  const parentForPublic: PlanType[] = [];
  const parentForCustom: PlanType[] = [];
  if (defaultPlan) {
    parentForPublic.push(defaultPlan);
    parentForCustom.push(defaultPlan);
  }
  publicPlans.forEach((p) => parentForCustom.push(p));

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

  const combinedPlans = [
    ...customPlans.map((plan) => ({
      plan,
      custom: true,
      ...excludePreviousPlanFeatures(plan, parentForCustom),
    })),
    ...publicPlans.map((plan) => {
      const featuresInfo = excludePreviousPlanFeatures(plan, parentForPublic);
      parentForPublic.push(plan);
      return {
        plan,
        custom: false,
        ...featuresInfo,
      };
    }),
  ];

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
      {combinedPlans.map((info) => {
        const { filteredFeatures, previousPlanName, plan, custom } = info;

        const parentPlan = previousPlanName;
        return (
          activeSubscription && (
            <Plan
              key={plan.id}
              plan={plan}
              active={isActive(plan)}
              ended={isEnded(plan)}
              onPeriodChange={onPeriodChange}
              period={period}
              filteredFeatures={filteredFeatures}
              featuresMinHeight="155px"
              custom={custom}
              nonCommercial={plan.nonCommercial}
              topFeature={
                parentPlan && <AllFromPlanFeature planName={parentPlan} />
              }
              action={
                <PlanAction
                  active={isActive(plan)}
                  ended={isEnded(plan)}
                  custom={custom}
                  show={!plan.free}
                  organizationHasSomeSubscription={
                    !activeSubscription.plan.free
                  }
                  period={period}
                  planId={plan.id}
                />
              }
            />
          )
        );
      })}
    </>
  );
};
