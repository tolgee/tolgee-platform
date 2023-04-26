import { useTranslate } from '@tolgee/react';
import { Box } from '@mui/material';

import { components } from 'tg.service/billingApiSchema.generated';
import { Plan, PlanContent } from 'tg.component/billing/plan/Plan';
import { useBillingApiMutation } from 'tg.service/http/useQueryApi';
import { useOrganization } from 'tg.views/organizations/useOrganization';
import { PlanTitle } from 'tg.component/billing/plan/PlanTitle';
import { PlanPrice } from 'tg.component/billing/plan/PlanPrice';
import { IncludedFeatures } from 'tg.component/billing/plan/IncludedFeatures';
import { PeriodSwitch } from 'tg.component/billing/plan/PeriodSwitch';
import { BillingPeriodType } from 'tg.component/billing/plan/PeriodSwitch';

import { PlanActionButton } from '../cloud/Plans/PlanActionButton';

export const SelfHostedEePlan = (props: {
  plan: components['schemas']['SelfHostedEePlanModel'];
  period: BillingPeriodType;
  onChange: (value: BillingPeriodType) => void;
}) => {
  const { t } = useTranslate();

  const hasFixedPrice = Boolean(
    props.plan.prices.subscriptionMonthly ||
      props.plan.prices.subscriptionYearly
  );
  const organization = useOrganization();

  const description = !hasFixedPrice
    ? t('billing_subscriptions_pay_for_what_you_use')
    : t('billing_subscriptions_pay_fixed_price', {
        includedSeats: props.plan.includedUsage.seats,
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

          <PlanPrice prices={props.plan.prices} period={props.period} />

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
