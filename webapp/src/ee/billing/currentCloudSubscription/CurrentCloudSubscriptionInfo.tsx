import { FC } from 'react';
import { T, useTranslate } from '@tolgee/react';
import { Alert, Badge, Box, styled } from '@mui/material';

import { components as billingComponents } from 'tg.service/billingApiSchema.generated';
import { components } from 'tg.service/apiSchema.generated';
import { useDateFormatter } from 'tg.hooks/useLocale';
import {
  StyledBillingSection,
  StyledBillingSectionSubtitle,
  StyledBillingSectionSubtitleSmall,
  StyledBillingSectionTitle,
} from '../BillingSection';
import { PlanMetric } from './PlanMetric';
import { MtHint } from 'tg.component/billing/MtHint';
import { StringsHint } from 'tg.component/common/StringsHint';
import { getProgressData } from '../component/utils';
import { BillingPeriodInfo } from './BillingPeriodInfo';
import { CloudEstimatedCosts } from './CloudEstimatedCosts';
import { SubscriptionsTrialAlert } from './SubscriptionsTrialAlert';

type CloudSubscriptionModel =
  billingComponents['schemas']['CloudSubscriptionModel'];
type UsageModel = components['schemas']['PublicUsageModel'];

const StyledContent = styled('div')`
  display: grid;
  grid-template-columns: auto auto 1fr;
  gap: 6px 16px;
  flex-wrap: wrap;
  margin: 4px 0px 32px 0px;
`;

type Props = {
  activeSubscription: CloudSubscriptionModel;
  usage: UsageModel;
};

export const CurrentCloudSubscriptionInfo: FC<Props> = ({
  activeSubscription,
  usage,
}) => {
  const { t } = useTranslate();
  const formatDate = useDateFormatter();

  const progressData = getProgressData(usage);

  const {
    translationsUsed,
    translationsMax,
    creditMax,
    creditUsed,
    isPayAsYouGo,
    usesSlots,
  } = progressData;

  const planName =
    activeSubscription.status === 'TRIALING' ? (
      <Badge
        badgeContent={t('subscriptions-actual-plan-trial-badge')}
        color="primary"
      >
        {activeSubscription.plan.name}
      </Badge>
    ) : (
      activeSubscription.plan.name
    );

  return (
    <>
      <StyledBillingSection gridArea="usage" maxWidth={750}>
        <StyledContent>
          <StyledBillingSectionTitle sx={{ mb: '12px', maxWidth: 260 }}>
            {t('billing_actual_title')}
          </StyledBillingSectionTitle>
          <StyledBillingSectionSubtitle>
            {planName}
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
              activeSubscription.estimatedCosts !== undefined &&
              activeSubscription.status !== 'TRIALING' && (
                <CloudEstimatedCosts
                  estimatedCosts={activeSubscription.estimatedCosts}
                />
              )}
          </Box>

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
          <BillingPeriodInfo subscription={activeSubscription} />
        </StyledContent>
      </StyledBillingSection>
      <SubscriptionsTrialAlert
        subscription={activeSubscription}
        usage={progressData}
      />
    </>
  );
};
