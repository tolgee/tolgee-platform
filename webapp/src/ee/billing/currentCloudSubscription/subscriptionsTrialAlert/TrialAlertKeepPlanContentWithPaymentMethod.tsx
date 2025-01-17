import { FC } from 'react';
import { SubscriptionsTrialAlertProps } from './SubscriptionsTrialAlert';
import { useDateFormatter } from 'tg.hooks/useLocale';
import { useRestoreCloudSubscription } from '../../Subscriptions/cloud/useRestoreCloudSubscription';
import { T } from '@tolgee/react';
import { Box } from '@mui/material';
import LoadingButton from 'tg.component/common/form/LoadingButton';
import { ReachingTheLimitMessage } from './ReachingTheLimitMessage';

export const TrialAlertKeepPlanContentWithPaymentMethod: FC<
  SubscriptionsTrialAlertProps
> = (props) => {
  const formatDate = useDateFormatter();

  const { restoreMutation, onRestore } = useRestoreCloudSubscription();

  return (
    <>
      <T
        keyName="billing-subscription-trial-cancelled-alert"
        params={{ trialEnd: formatDate(props.subscription.trialEnd), b: <b /> }}
      />
      <Box sx={{ mt: 2 }}>
        <LoadingButton
          color="primary"
          variant="contained"
          loading={restoreMutation.isLoading}
          onClick={onRestore}
          data-cy={'billing-subscription-trial-alert-keep-button'}
        >
          <T keyName="billing-subscription-trial-alert-keep-button" />
        </LoadingButton>
      </Box>
      <ReachingTheLimitMessage {...props}></ReachingTheLimitMessage>
    </>
  );
};
