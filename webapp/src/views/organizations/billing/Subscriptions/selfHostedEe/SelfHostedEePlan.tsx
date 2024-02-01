import { T, useTranslate } from '@tolgee/react';
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

  const hasPrice = Boolean(
    props.plan.prices.subscriptionMonthly ||
      props.plan.prices.subscriptionYearly
  );
  const organization = useOrganization();

  function getDescription() {
    if (props.plan.free) {
      return <T keyName="billing_subscriptions_free_plan_description" />;
    }

    if (!hasPrice) {
      return (
        <T
          keyName="billing_subscriptions_pay_for_what_you_use"
          params={{
            includedSeats: props.plan.includedUsage.seats,
            includedMtCredits: props.plan.includedUsage.mtCredits,
            b: (chunks) => <b>{chunks}</b>,
          }}
        />
      );
    }

    return null;
  }

  const description = getDescription();

  const subscribeMutation = useBillingApiMutation({
    url: '/v2/organizations/{organizationId}/billing/self-hosted-ee/subscriptions',
    method: 'post',
    options: {
      onSuccess: (data) => {
        window.location.href = data.url;
      },
    },
  });

  const subscribeFreeMutation = useBillingApiMutation({
    url: '/v2/organizations/{organizationId}/billing/self-hosted-ee/subscribe-free',
    method: 'post',
    options: {
      onSuccess: () => {},
    },
    invalidatePrefix: '/v2/organizations/{organizationId}/billing',
  });

  const onSubscribe = () => {
    if (props.plan.free) {
      subscribeFreeMutation.mutate({
        path: { organizationId: organization!.id },
        content: {
          'application/json': {
            planId: props.plan.id,
          },
        },
      });
      return;
    }
    subscribeMutation.mutate({
      path: { organizationId: organization!.id },
      content: {
        'application/json': {
          planId: props.plan.id,
          period: props.period,
        },
      },
    });
  };

  return (
    <>
      <Plan data-cy="billing-self-hosted-ee-plan">
        <PlanContent>
          <PlanTitle title={props.plan.name}></PlanTitle>

          <Box gridArea="info">
            <Box>{description}</Box>
            <IncludedFeatures
              features={props.plan.enabledFeatures}
              includedUsage={props.plan.includedUsage}
            />
          </Box>

          {hasPrice && (
            <PeriodSwitch value={props.period} onChange={props.onChange} />
          )}

          <PlanPrice prices={props.plan.prices} period={props.period} />

          <PlanActionButton
            data-cy="billing-self-hosted-ee-plan-subscribe-button"
            loading={subscribeMutation.isLoading}
            onClick={onSubscribe}
          >
            {t('billing_plan_subscribe')}
          </PlanActionButton>
        </PlanContent>
      </Plan>
    </>
  );
};
