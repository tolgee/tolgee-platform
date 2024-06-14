import { useTranslate } from '@tolgee/react';
import { Box } from '@mui/material';

import { components } from 'tg.service/billingApiSchema.generated';
import { useOrganization } from '../../../useOrganization';
import { Plan, PlanContent } from '../common/Plan';
import { PlanTitle } from '../common/PlanTitle';
import { useBillingApiMutation } from 'tg.service/http/useQueryApi';
import { IncludedFeatures } from './IncludedFeatures';
import {
  BillingPeriodType,
  PeriodSwitch,
} from '../cloud/Plans/Price/PeriodSwitch';
import { PlanDescription } from './PlanDescription';
import { PlanPrice } from '../cloud/Plans/Price/PlanPrice';
import LoadingButton from 'tg.component/common/form/LoadingButton';

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

          <Box>
            <Box>
              <PlanDescription hasPrice={hasPrice} free={props.plan.free} />
            </Box>
            <IncludedFeatures features={props.plan.enabledFeatures} />
          </Box>

          {hasPrice && (
            <PeriodSwitch value={props.period} onChange={props.onChange} />
          )}

          <PlanPrice
            prices={props.plan.prices}
            period={props.period}
            onPeriodChange={props.onChange}
          />

          <LoadingButton
            data-cy="billing-self-hosted-ee-plan-subscribe-button"
            loading={subscribeMutation.isLoading}
            onClick={onSubscribe}
          >
            {t('billing_plan_subscribe')}
          </LoadingButton>
        </PlanContent>
      </Plan>
    </>
  );
};
