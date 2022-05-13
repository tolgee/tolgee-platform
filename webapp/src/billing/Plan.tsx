import LoadingButton from 'tg.component/common/form/LoadingButton';
import { useUpgradeSubscription } from './useUpgradeSubscription';
import { FC } from 'react';
import { components } from 'tg.service/billingApiSchema.generated';
import { PlanInfo } from './PlanInfo';
import { useBillingApiMutation } from './useBillingQueryApi';
import { T } from '@tolgee/react';
import { useOrganization } from 'tg.views/organizations/useOrganization';
import { container } from 'tsyringe';
import { MessageService } from 'tg.service/MessageService';

const messaging = container.resolve(MessageService);
export const Plan: FC<{
  plan: components['schemas']['PlanModel'];
  isOrganizationSubscribed: boolean;
}> = (props) => {
  const { upgradeMutation, onUpgrade } = useUpgradeSubscription();

  const organization = useOrganization();

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
