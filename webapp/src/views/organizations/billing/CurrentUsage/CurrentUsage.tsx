import { FC } from 'react';
import { T, useTranslate } from '@tolgee/react';
import { Box, styled } from '@mui/material';

import { components as billingComponents } from 'tg.service/billingApiSchema.generated';
import { components } from 'tg.service/apiSchema.generated';
import { useDateFormatter } from 'tg.hooks/useLocale';
import {
  StyledBillingSection,
  StyledBillingSectionSubtitle,
  StyledBillingSectionSubtitleSmall,
  StyledBillingSectionTitle,
} from '../BillingSection';
import { PlanMetric, StyledMetrics } from './PlanMetric';
import { MtHint } from 'tg.component/billing/MtHint';
import { EstimatedCosts } from '../common/usage/EstimatedCosts';
import { useBillingApiQuery } from 'tg.service/http/useQueryApi';
import { useOrganization } from '../../useOrganization';
import { getProgressData } from 'tg.component/billing/utils';
import { StringsHint } from 'tg.component/billing/StringsHint';

type CloudSubscriptionModel =
  billingComponents['schemas']['CloudSubscriptionModel'];
type UsageModel = components['schemas']['PublicUsageModel'];
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
  activeSubscription: CloudSubscriptionModel;
  usage: UsageModel;
  balance: CreditBalanceModel;
};

export const CurrentUsage: FC<Props> = ({
  activeSubscription,
  usage,
  balance,
}) => {
  const { t } = useTranslate();
  const formatDate = useDateFormatter();

  const {
    translationsUsed,
    translationsMax,
    creditMax,
    creditUsed,
    isPayAsYouGo,
    usesSlots,
  } = getProgressData(usage);

  return (
    <StyledBillingSection gridArea="usage" maxWidth={650}>
      <StyledHeader>
        <StyledBillingSectionTitle>
          {t('billing_actual_title')}
        </StyledBillingSectionTitle>
        <StyledBillingSectionSubtitle>
          {activeSubscription.plan.name}
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
        <Box flexGrow={1}>
          {activeSubscription.plan.type === 'PAY_AS_YOU_GO' &&
            activeSubscription.estimatedCosts !== undefined && (
              <CloudEstimatedCosts
                estimatedCosts={activeSubscription.estimatedCosts}
              />
            )}
        </Box>
      </StyledHeader>
      <StyledMetrics>
        <PlanMetric
          name={
            usesSlots ? (
              t('billing_actual_used_translations')
            ) : (
              <T
                keyName="billing_actual_used_strings_with_hint"
                params={{ hint: <StringsHint /> }}
              />
            )
          }
          currentQuantity={translationsUsed}
          totalQuantity={translationsMax}
          periodEnd={activeSubscription.currentPeriodEnd}
          isPayAsYouGo={isPayAsYouGo}
          data-cy="billing-actual-used-strings"
        />
        <PlanMetric
          name={
            <T
              keyName="billing_actual_used_monthly_credits"
              params={{ hint: <MtHint /> }}
            />
          }
          currentQuantity={creditUsed}
          totalQuantity={creditMax || 0}
          periodEnd={activeSubscription.currentPeriodEnd}
          isPayAsYouGo={isPayAsYouGo}
          data-cy="billing-actual-used-monthly-credits"
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
          currentQuantity={usage.extraCreditBalance || 0}
        />
        {!activeSubscription.plan.free && (
          <>
            <Box gridColumn="1">{t('billing_actual_period')}</Box>
            <Box gridColumn="2 / -1" data-cy="billing-actual-period">
              {activeSubscription.currentBillingPeriod === 'MONTHLY'
                ? t('billing_monthly')
                : t('billing_annual')}
            </Box>
            <Box gridColumn="1">{t('billing_actual_period_end')}</Box>
            <Box gridColumn="2 / -1" data-cy="billing-actual-period-end">
              {formatDate(activeSubscription.currentPeriodEnd)} (
              {!activeSubscription.cancelAtPeriodEnd ? (
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

const CloudEstimatedCosts: FC<{ estimatedCosts: number }> = (props) => {
  const organization = useOrganization();

  const useUsage = (enabled: boolean) =>
    useBillingApiQuery({
      url: '/v2/organizations/{organizationId}/billing/expected-usage',
      method: 'get',
      path: {
        organizationId: organization!.id,
      },
      options: {
        enabled,
      },
    });

  return <EstimatedCosts {...props} useUsage={useUsage} />;
};
