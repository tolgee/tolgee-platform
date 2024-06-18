import { styled } from '@mui/material';
import { useTranslate } from '@tolgee/react';
import { PlanFeature } from 'tg.component/billing/PlanFeature';

import { BillingPeriodType } from './cloud/Plans/Price/PeriodSwitch';
import { CloudPlan } from './cloud/Plans/CloudPlan';
import { PlanType } from './cloud/Plans/types';
import { excludePreviousPlanFeatures } from './common/plansTools';
import { AllFromPlanFeature } from './common/AllFromPlanFeature';
import { SelfHostedPlanAction } from './selfHostedEe/SelfHostedPlanAction';

const StyledPlanWrapper = styled('div')`
  display: grid;
`;

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
  const paidPlans = [...plans];
  const { t } = useTranslate();
  // add enterprise plan
  paidPlans.push({
    id: -1,
    type: 'CONTACT_US',
    name: 'Enterprise',
    enabledFeatures: [
      'ACCOUNT_MANAGER',
      'PREMIUM_SUPPORT',
      'DEDICATED_SLACK_CHANNEL',
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
    ],
    free: false,
    hasYearlyPrice: false,
    public: true,
  });

  const prevPaidPlans: PlanType[] = [];

  return (
    <>
      {paidPlans.map((plan) => {
        prevPaidPlans.push(plan);

        const { filteredFeatures, previousPlanName } =
          excludePreviousPlanFeatures(prevPaidPlans);

        return (
          <StyledPlanWrapper key={plan.id}>
            <CloudPlan
              plan={plan}
              isActive={false}
              isEnded={false}
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
              action={<SelfHostedPlanAction plan={plan} period={period} />}
            />
          </StyledPlanWrapper>
        );
      })}
    </>
  );
};
