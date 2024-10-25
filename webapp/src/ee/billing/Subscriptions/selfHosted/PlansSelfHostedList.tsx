import { useTranslate } from '@tolgee/react';
import { PlanFeature } from 'tg.component/billing/PlanFeature';
import { BillingPeriodType } from 'tg.component/billing/Price/PeriodSwitch';
import { Plan } from 'tg.component/billing/Plan/Plan';
import { PlanType } from 'tg.component/billing/Plan/types';
import { excludePreviousPlanFeatures } from 'tg.component/billing/Plan/plansTools';
import { AllFromPlanFeature } from 'tg.component/billing/Plan/AllFromPlanFeature';

import { SelfHostedPlanAction } from './SelfHostedPlanAction';

type BillingPlansProps = {
  plans: PlanType[];
  period: BillingPeriodType;
  onPeriodChange: (period: BillingPeriodType) => void;
};

export const PlansSelfHostedList: React.FC<BillingPlansProps> = ({
  plans,
  period,
  onPeriodChange,
}) => {
  const { t } = useTranslate();
  const publicPlans = plans.filter((p) => p.public);
  const customPlans = plans.filter((p) => !p.public);

  // add enterprise plan
  publicPlans.push({
    id: -1,
    type: 'CONTACT_US',
    name: 'Enterprise',
    enabledFeatures: [
      'ACCOUNT_MANAGER',
      'PREMIUM_SUPPORT',
      'DEDICATED_SLACK_CHANNEL',
      'SLACK_INTEGRATION',
      'DEPLOYMENT_ASSISTANCE',
      'ASSISTED_UPDATES',
      'BACKUP_CONFIGURATION',
      'TEAM_TRAINING',
      'AI_PROMPT_CUSTOMIZATION',
      'GRANULAR_PERMISSIONS',
      'MULTIPLE_CONTENT_DELIVERY_CONFIGS',
      'PRIORITIZED_FEATURE_REQUESTS',
      'PROJECT_LEVEL_CONTENT_STORAGES',
      'STANDARD_SUPPORT',
      'WEBHOOKS',
      'TASKS',
    ],
    free: false,
    hasYearlyPrice: false,
    public: true,
  });

  const parentForPublic: PlanType[] = [];
  const parentForCustom: PlanType[] = publicPlans;

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
      {combinedPlans.map((info) => {
        const { filteredFeatures, previousPlanName, custom, plan } = info;

        return (
          <Plan
            key={plan.id}
            plan={plan}
            active={false}
            ended={false}
            custom={custom}
            onPeriodChange={onPeriodChange}
            period={period}
            filteredFeatures={filteredFeatures}
            featuresMinHeight="210px"
            topFeature={
              previousPlanName ? (
                <AllFromPlanFeature planName={previousPlanName} />
              ) : (
                <PlanFeature
                  bold
                  link="https://tolgee.io/pricing/self-hosted#features-table"
                  name={t('billing_subscriptions_all_essentials')}
                />
              )
            }
            action={
              <SelfHostedPlanAction
                plan={plan}
                period={period}
                custom={custom}
              />
            }
          />
        );
      })}
    </>
  );
};
