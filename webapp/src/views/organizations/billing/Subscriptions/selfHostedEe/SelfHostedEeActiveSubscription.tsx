import { FC } from 'react';
import { components } from 'tg.service/billingApiSchema.generated';
import { CancelSelfHostedEeSubscriptionButton } from '../../CancelSelfHostedEeSubscriptionButton';
import { IncludedFeatures } from './IncludedFeatures';
import { Plan, PlanContent } from '../common/Plan';
import { PlanPrice } from '../cloud/Plans/PlanPrice';
import { PlanInfoArea } from '../common/PlanInfo';
import { SelfHostedEeEstimatedCosts } from './SelfHostedEeEstimatedCosts';
import { ActivePlanTitle } from './ActivePlanTitle';
import { useTranslate } from '@tolgee/react';
import { Box } from '@mui/material';

export const SelfHostedEeActiveSubscription: FC<{
  subscription: components['schemas']['SelfHostedEeSubscriptionModel'];
}> = ({ subscription }) => {
  const period = subscription.currentBillingPeriod;
  const { t } = useTranslate();

  const hasFixedPrice = Boolean(
    subscription.plan.monthlyPrice || subscription.plan.yearlyPrice
  );

  const description = !hasFixedPrice
    ? t('billing_subscriptions_pay_for_what_you_use')
    : t('billing_subscriptions_pay_fixed_price', {
        includedSeats: subscription.plan.includedSeats,
      });

  const price =
    period === 'MONTHLY'
      ? subscription.plan.monthlyPrice
      : subscription.plan.yearlyPrice;

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
          licenseKey={subscription.licenseKey}
        />

        <SelfHostedEeEstimatedCosts subscription={subscription} />

        <PlanInfoArea>
          <Box>{description}</Box>
          <IncludedFeatures features={subscription.plan.enabledFeatures} />
        </PlanInfoArea>

        <PlanPrice
          pricePerSeat={subscription.plan.pricePerSeat}
          subscriptionPrice={price}
          period={period}
        />
        <CancelSelfHostedEeSubscriptionButton id={subscription.id} />
      </PlanContent>
    </Plan>
  );
};
