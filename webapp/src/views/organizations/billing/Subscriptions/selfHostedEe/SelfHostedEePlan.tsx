import { useTranslate } from '@tolgee/react';
import { Box } from '@mui/material';

import { components } from 'tg.service/billingApiSchema.generated';
import { useOrganization } from '../../../useOrganization';
import { Plan, PlanContent } from '../common/Plan';
import { PlanTitle } from '../common/PlanTitle';
import { PlanActionButton } from '../cloud/Plans/PlanActionButton';
import { PlanPrice } from '../cloud/Plans/PlanPrice';
import { useBillingApiMutation } from 'tg.service/http/useQueryApi';
import { IncludedFeatures } from './IncludedFeatures';
import { BillingPeriodType, PeriodSwitch } from '../cloud/Plans/PeriodSwitch';

export const SelfHostedEePlan = (props: {
  plan: components['schemas']['SelfHostedEePlanModel'];
  period: BillingPeriodType;
  onChange: (value: BillingPeriodType) => void;
}) => {
  const { t } = useTranslate();

  const hasFixedPrice = Boolean(
    props.plan.monthlyPrice || props.plan.yearlyPrice
  );
  const organization = useOrganization();

  const price =
    props.period === 'MONTHLY'
      ? props.plan.monthlyPrice
      : props.plan.yearlyPrice;

  const description = !hasFixedPrice
    ? t('billing_subscriptions_pay_for_what_you_use')
    : t('billing_subscriptions_pay_fixed_price', {
        includedSeats: props.plan.includedSeats,
      });

  const subscribeMutation = useBillingApiMutation({
    url: '/v2/organizations/{organizationId}/billing/self-hosted-ee/subscriptions',
    method: 'post',
    options: {
      onSuccess: (data) => {
        window.location.href = data.url;
      },
    },
  });

  return (
    <>
      <Plan>
        <PlanContent>
          <PlanTitle title={props.plan.name}></PlanTitle>

          <Box gridArea="info">
            <Box>{description}</Box>
            <IncludedFeatures features={props.plan.enabledFeatures} />
          </Box>

          {hasFixedPrice && (
            <PeriodSwitch value={props.period} onChange={props.onChange} />
          )}

          <PlanPrice
            pricePerSeat={props.plan.pricePerSeat}
            subscriptionPrice={price}
            period={props.period}
          />

          <PlanActionButton
            loading={subscribeMutation.isLoading}
            onClick={() =>
              subscribeMutation.mutate({
                path: { organizationId: organization!.id },
                content: {
                  'application/json': {
                    planId: props.plan.id,
                    period: props.period,
                  },
                },
              })
            }
          >
            {t('billing_plan_subscribe')}
          </PlanActionButton>
        </PlanContent>
      </Plan>
    </>
  );
};
