import { components as billingComponents } from 'tg.service/billingApiSchema.generated';
import { components } from 'tg.service/apiSchema.generated';
import { BillingSection } from '../BillingSection';
import { PlanMetric, StyledMetrics } from './PlanMetric';
import { useTranslate } from '@tolgee/react';

type ActivePlanModel = billingComponents['schemas']['ActivePlanModel'];
type UsageModel = components['schemas']['UsageModel'];
type CreditBalanceModel = components['schemas']['CreditBalanceModel'];

type Props = {
  activePlan: ActivePlanModel;
  usage: UsageModel;
  balance: CreditBalanceModel;
};

export const ActivePlan: React.FC<Props> = ({ activePlan, usage, balance }) => {
  const t = useTranslate();
  return (
    <BillingSection
      title={t('billing_actual_title')}
      subtitle={`${activePlan.name}`}
    >
      <StyledMetrics>
        <PlanMetric
          name={t('billing_actual_available_translations')}
          currentAmount={usage.translationLimit - usage.currentTranslations}
          totalAmount={usage.translationLimit}
          periodEnd={activePlan.currentPeriodEnd}
        />
        <PlanMetric
          name={t('billing_actual_monthly_credits')}
          currentAmount={balance.creditBalance || 0}
          totalAmount={activePlan.includedMtCredits || 0}
          periodEnd={activePlan.currentPeriodEnd}
        />
        <PlanMetric
          name={t('billing_actual_extra_credits')}
          currentAmount={balance.extraCreditBalance || 0}
        />
      </StyledMetrics>
    </BillingSection>
  );
};
