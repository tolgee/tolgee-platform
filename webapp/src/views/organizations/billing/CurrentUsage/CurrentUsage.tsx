import { FC } from 'react';
import { useTranslate, T } from '@tolgee/react';
import { Box, styled } from '@mui/material';

import { components as billingComponents } from 'tg.service/billingApiSchema.generated';
import { components } from 'tg.service/apiSchema.generated';
import { useDateFormatter } from 'tg.hooks/useLocale';
import {
  StyledBillingSection,
  StyledBillingSectionTitle,
  StyledBillingSectionSubtitle,
  StyledBillingSectionSubtitleSmall,
} from '../BillingSection';
import { PlanMetric, StyledMetrics } from './PlanMetric';
import { MtHint } from 'tg.component/billing/MtHint';

type ActivePlanModel = billingComponents['schemas']['ActivePlanModel'];
type UsageModel = components['schemas']['UsageModel'];
type CreditBalanceModel = components['schemas']['CreditBalanceModel'];

const StyledPositive = styled('span')`
  color: ${({ theme }) => theme.palette.success.main};
`;

const StyledNegative = styled('span')`
  color: ${({ theme }) => theme.palette.error.main};
`;

const StyledHeader = styled('div')`
  display: flex;
  align-items: baseline;
  gap: 0px 24px;
  flex-wrap: wrap;
`;

type Props = {
  activePlan: ActivePlanModel;
  usage: UsageModel;
  balance: CreditBalanceModel;
};

export const CurrentUsage: FC<Props> = ({ activePlan, usage, balance }) => {
  const { t } = useTranslate();
  const formatDate = useDateFormatter();
  return (
    <StyledBillingSection gridArea="usage">
      <StyledHeader>
        <StyledBillingSectionTitle>
          {t('billing_actual_title')}
        </StyledBillingSectionTitle>
        <StyledBillingSectionSubtitle>
          {activePlan.name}
          {Boolean(usage.extraCreditBalance) && (
            <StyledBillingSectionSubtitleSmall>
              {' '}
              +{' '}
              <T
                keyName="billing_actual_extra_credits"
                params={{ hint: <MtHint /> }}
              />
            </StyledBillingSectionSubtitleSmall>
          )}
        </StyledBillingSectionSubtitle>
      </StyledHeader>
      <StyledMetrics>
        <PlanMetric
          name={t('billing_actual_available_translations')}
          currentAmount={usage.translationLimit - usage.currentTranslations}
          totalAmount={usage.translationLimit}
          periodEnd={activePlan.currentPeriodEnd}
        />
        <PlanMetric
          name={
            <T
              keyName="billing_actual_monthly_credits"
              params={{ hint: <MtHint /> }}
            />
          }
          currentAmount={Math.round(usage.creditBalance / 100)}
          totalAmount={Math.round((usage.includedMtCredits || 0) / 100)}
          periodEnd={activePlan.currentPeriodEnd}
        />
        <Box gridColumn="1">{t('billing_credits_refill')}</Box>
        <Box gridColumn="2 / -1" data-cy="billing-actual-period">
          {formatDate(usage.creditBalanceNextRefillAt)}
        </Box>
        <PlanMetric
          data-cy="billing-actual-extra-credits"
          name={
            <T
              keyName="billing_actual_extra_credits"
              params={{ hint: <MtHint /> }}
            />
          }
          currentAmount={Math.round((usage.extraCreditBalance || 0) / 100)}
        />
        {!activePlan.free && (
          <>
            <Box gridColumn="1">{t('billing_actual_period')}</Box>
            <Box gridColumn="2 / -1" data-cy="billing-actual-period">
              {activePlan.currentBillingPeriod === 'MONTHLY'
                ? t('billing_monthly')
                : t('billing_annual')}
            </Box>
            <Box gridColumn="1">{t('billing_actual_period_end')}</Box>
            <Box gridColumn="2 / -1" data-cy="billing-actual-period-end">
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
    </StyledBillingSection>
  );
};
