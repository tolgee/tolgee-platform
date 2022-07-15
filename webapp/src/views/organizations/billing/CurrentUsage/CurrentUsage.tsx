import { useTranslate } from '@tolgee/react';
import { Box, styled } from '@mui/material';

import { components as billingComponents } from 'tg.service/billingApiSchema.generated';
import { components } from 'tg.service/apiSchema.generated';
import { useDateFormatter } from 'tg.hooks/useLocale';
import { BillingSection } from '../BillingSection';
import { PlanMetric, StyledMetrics } from './PlanMetric';
import { FC } from 'react';

type ActivePlanModel = billingComponents['schemas']['ActivePlanModel'];
type UsageModel = components['schemas']['UsageModel'];
type CreditBalanceModel = components['schemas']['CreditBalanceModel'];

const StyledPositive = styled('span')`
  color: ${({ theme }) => theme.palette.success.main};
`;

const StyledNegative = styled('span')`
  color: ${({ theme }) => theme.palette.error.main};
`;

type Props = {
  activePlan: ActivePlanModel;
  usage: UsageModel;
  balance: CreditBalanceModel;
};

export const CurrentUsage: FC<Props> = ({ activePlan, usage, balance }) => {
  const t = useTranslate();
  const formatDate = useDateFormatter();
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
          currentAmount={usage.creditBalance / 100}
          totalAmount={(usage.includedMtCredits || 0) / 100}
          periodEnd={activePlan.currentPeriodEnd}
        />
        <PlanMetric
          name={t('billing_actual_extra_credits')}
          currentAmount={usage.extraCreditBalance || 0}
        />
        {!activePlan.free && (
          <>
            <Box gridColumn="1">{t('billing_actual_period_end')}</Box>
            <Box gridColumn="2 / -1">
              {formatDate(activePlan.currentPeriodEnd)} (
              {!activePlan.cancelAtPeriodEnd ? (
                <StyledPositive>
                  {t('billing_actual_period_renewal')}
                </StyledPositive>
              ) : (
                <StyledNegative>
                  {t('billing_actual_period_finish')}
                </StyledNegative>
              )}
              )
            </Box>
          </>
        )}
      </StyledMetrics>
    </BillingSection>
  );
};
