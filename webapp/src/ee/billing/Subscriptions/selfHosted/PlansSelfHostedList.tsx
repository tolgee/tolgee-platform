import { useTranslate } from '@tolgee/react';

import { SelfHostedPlanAction } from './SelfHostedPlanAction';
import { BillingPeriodType } from '../../component/Price/PeriodSwitch';
import { excludePreviousPlanFeatures } from '../../component/Plan/plansTools';
import { Plan } from 'tg.ee.module/billing/component/Plan/Plan';
import { AllFromPlanFeature } from 'tg.ee.module/billing/component/Plan/AllFromPlanFeature';
import { PlanFeature } from 'tg.ee.module/billing/component/PlanFeature';
import { components } from 'tg.service/billingApiSchema.generated';
import React, { FC } from 'react';
import { PlanType } from '../../component/Plan/types';

export type SelfHostedEePlanModel =
  components['schemas']['SelfHostedEePlanModel'];

type BillingPlansProps = {
  plans: PlanType[];
  period: BillingPeriodType;
  onPeriodChange: (period: BillingPeriodType) => void;
};

export const PlansSelfHostedList: FC<BillingPlansProps> = ({
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
      'GRANULAR_PERMISSIONS',
      'MULTIPLE_CONTENT_DELIVERY_CONFIGS',
      'PRIORITIZED_FEATURE_REQUESTS',
      'PROJECT_LEVEL_CONTENT_STORAGES',
      'STANDARD_SUPPORT',
      'WEBHOOKS',
      'TASKS',
      'ORDER_TRANSLATION',
      'SSO',
      'AI_PROMPT_CUSTOMIZATION',
      'GLOSSARY',
      'TRANSLATION_LABELS',
    ] as const satisfies SelfHostedEePlanModel['enabledFeatures'],
    includedUsage: {
      seats: -2,
      keys: -2,
      mtCredits: -2,
      translations: -2,
    },
    free: false,
    hasYearlyPrice: false,
    public: true,
    nonCommercial: false,
    metricType: 'KEYS_SEATS',
    archivedAt: undefined,
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
            nonCommercial={plan.nonCommercial}
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
