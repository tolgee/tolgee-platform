import { FC } from 'react';
import { useTranslate } from '@tolgee/react';
import { Box } from '@mui/material';

import { components } from 'tg.service/billingApiSchema.generated';
import { Plan, PlanContent } from 'tg.component/billing/plan/Plan';
import { PlanInfoArea } from 'tg.component/billing/plan/PlanInfo';
import { PlanPrice } from 'tg.component/billing/plan/PlanPrice';
import { IncludedFeatures } from 'tg.component/billing/plan/IncludedFeatures';

import { SelfHostedEeSubscriptionActions } from '../../SelfHostedEeSubscriptionActions';
import { SelfHostedEeEstimatedCosts } from './SelfHostedEeEstimatedCosts';
import { ActivePlanTitle } from './ActivePlanTitle';

export const SelfHostedEeActiveSubscription: FC<{
  subscription: components['schemas']['SelfHostedEeSubscriptionModel'];
}> = ({ subscription }) => {
  const period = subscription.currentBillingPeriod;
  const { t } = useTranslate();

  const hasFixedPrice = Boolean(
    subscription.plan.prices.subscriptionMonthly ||
      subscription.plan.prices.subscriptionYearly
  );

  const description = !hasFixedPrice
    ? t('billing_subscriptions_pay_for_what_you_use')
    : t('billing_subscriptions_pay_fixed_price', {
        includedSeats: subscription.plan.includedUsage.seats,
      });

  return (
    <Plan
      sx={(theme) => ({
        border: `1px solid #c39dae`,
      })}
    >
      <PlanContent>
        <ActivePlanTitle
          name={subscription.plan.name}
          status={subscription.status}
          createdAt={subscription.createdAt}
          periodStart={subscription.currentPeriodStart}
          periodEnd={subscription.currentPeriodEnd}
        />

        <SelfHostedEeEstimatedCosts subscription={subscription} />

        <PlanInfoArea>
          <Box>{description}</Box>
          <IncludedFeatures features={subscription.plan.enabledFeatures} />
        </PlanInfoArea>

        <PlanPrice prices={subscription.plan.prices} period={period} />

        <SelfHostedEeSubscriptionActions
          id={subscription.id}
          licenceKey={subscription.licenseKey}
        />
      </PlanContent>
    </Plan>
  );
};
