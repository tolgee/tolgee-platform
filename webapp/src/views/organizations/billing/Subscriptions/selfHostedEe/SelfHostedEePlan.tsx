import { components } from 'tg.service/billingApiSchema.generated';
import { Plan, PlanContent } from '../common/Plan';
import { PlanTitle } from '../common/PlanTitle';
import { PlanActionButton } from '../cloud/Plans/PlanActionButton';
import { useTranslate } from '@tolgee/react';
import { PlanPrice } from '../cloud/Plans/PlanPrice';
import { Box } from '@mui/material';
import { useBillingApiMutation } from 'tg.service/http/useQueryApi';
import { useOrganization } from '../../../useOrganization';
import { IncludedFeatures } from './IncludedFeatures';

export const SelfHostedEePlan = (props: {
  plan: components['schemas']['SelfHostedEePlanModel'];
}) => {
  const { t } = useTranslate();

  const organization = useOrganization();

  const description =
    props.plan.subscriptionPrice == 0
      ? t('billing_subscriptions_pay_for_what_you_use')
      : t('billing_subscriptions_pay_fixed_price', {
          includedSeats: props.plan.includedSeats,
        });

  const subscribeMutation = useBillingApiMutation({
    url: '/v2/organizations/{organizationId}/billing/setup-ee',
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

          <Box sx={{ gridArea: 'info' }}>
            <Box>{description}</Box>
            <IncludedFeatures plan={props.plan} />
          </Box>
          <PlanPrice
            pricePerSeat={props.plan.pricePerSeat}
            subscriptionPrice={props.plan.subscriptionPrice}
          />
          <PlanActionButton
            loading={subscribeMutation.isLoading}
            onClick={() =>
              subscribeMutation.mutate({
                path: { organizationId: organization!.id },
                content: {
                  'application/json': {
                    planId: props.plan.id,
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
