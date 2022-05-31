import { Box } from '@mui/material';
import LoadingButton from 'tg.component/common/form/LoadingButton';
import { FC } from 'react';
import { components } from 'tg.service/billingApiSchema.generated';
import { useCurrentLanguage } from '@tolgee/react';
import { useBillingApiMutation } from './useBillingQueryApi';
import { useOrganization } from 'tg.views/organizations/useOrganization';
import { PlanInfo } from './PlanInfo';
import { useUpgradeSubscription } from './useUpgradeSubscription';

export const ActivePlan: FC<{
  plan: components['schemas']['ActivePlanModel'];
  period: components['schemas']['SubscribeRequest']['period'];
}> = (props) => {
  const organization = useOrganization();

  const getCurrentLang = useCurrentLanguage();

  const cancelMutation = useBillingApiMutation({
    url: '/v2/organizations/{organizationId}/billing/cancel-subscription',
    method: 'post',
    invalidatePrefix: '/v2/billing/active-plan',
  });

  const onCancel = () => {
    cancelMutation.mutate({ path: { organizationId: organization!.id } });
  };

  const { upgradeMutation, onUpgrade } = useUpgradeSubscription(
    props.plan.id,
    props.period
  );

  return (
    <>
      <PlanInfo plan={props.plan} />
      This is Active
      <Box>
        Period end:{' '}
        {props.plan.currentPeriodEnd
          ? new Date(props.plan.currentPeriodEnd).toLocaleDateString(
              getCurrentLang()
            )
          : '-'}
      </Box>
      <Box>
        Cancel at period end: {props.plan.cancelAtPeriodEnd ? 'true' : 'false'}
      </Box>
      {!props.plan.free &&
        (props.plan.cancelAtPeriodEnd ? (
          <LoadingButton
            loading={upgradeMutation.isLoading}
            variant="outlined"
            color="primary"
            onClick={() => onUpgrade()}
          >
            Subscribe
          </LoadingButton>
        ) : (
          <LoadingButton
            loading={cancelMutation.isLoading}
            variant="outlined"
            color="primary"
            onClick={() => onCancel()}
          >
            Cancel subscription
          </LoadingButton>
        ))}
    </>
  );
};
