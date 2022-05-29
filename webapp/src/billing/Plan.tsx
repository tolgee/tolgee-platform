import LoadingButton from 'tg.component/common/form/LoadingButton';
import { useUpgradeSubscription } from './useUpgradeSubscription';
import { FC } from 'react';
import { components } from 'tg.service/billingApiSchema.generated';
import { PlanInfo } from './PlanInfo';
import { useBillingApiMutation } from './useBillingQueryApi';
import { T, useTranslate } from '@tolgee/react';
import { useOrganization } from 'tg.views/organizations/useOrganization';
import { container } from 'tsyringe';
import { MessageService } from 'tg.service/MessageService';
import { Box } from '@mui/material';

const messaging = container.resolve(MessageService);
export const Plan: FC<{
  plan: components['schemas']['PlanModel'];
  isOrganizationSubscribed: boolean;
  period: 'monthly' | 'yearly';
}> = (props) => {
  const { upgradeMutation, onUpgrade } = useUpgradeSubscription();

  const organization = useOrganization();
  const t = useTranslate();

  const onSubscribe = (planId: number) => {
    subscribeMutation.mutate({
      path: {
        organizationId: organization!.id,
      },
      content: {
        'application/json': {
          planId: planId,
        },
      },
    });
  };

  const subscribeMutation = useBillingApiMutation({
    url: '/v2/organizations/{organizationId}/billing/subscribe',
    method: 'post',
    invalidatePrefix: '/v2/organizations/{organizationId}/billing',
    options: {
      onSuccess: (data) => {
        window.location.href = data;
      },
      onError: (data) => {
        if (data.code === 'organization_already_subscribed') {
          messaging.error(
            <T keyName="billing_organization_already_subscribed" />
          );
        }
      },
    },
  });

  return (
    <>
      <PlanInfo plan={props.plan} />
      <Box>
        {t({
          key: 'billing_plan_price',
          defaultValue: 'Price: {price, number, :: currency/EUR}',
          parameters: {
            price:
              ((props.period === 'monthly'
                ? props.plan.monthlyPrice
                : props.plan.yearlyPrice) || 0) / 100,
          },
        })}
      </Box>

      {!props.plan.free &&
        (props.isOrganizationSubscribed ? (
          <LoadingButton
            loading={upgradeMutation.isLoading}
            variant="outlined"
            color="primary"
            onClick={() => onUpgrade(props.plan.id)}
          >
            Subscribe
          </LoadingButton>
        ) : (
          <LoadingButton
            loading={subscribeMutation.isLoading}
            variant="outlined"
            color="primary"
            onClick={() => onSubscribe(props.plan.id)}
          >
            Subscribe
          </LoadingButton>
        ))}
    </>
  );
};
