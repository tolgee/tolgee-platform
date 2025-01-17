import { FC } from 'react';
import { SubscriptionsTrialAlertProps } from './SubscriptionsTrialAlert';
import { useDateFormatter } from 'tg.hooks/useLocale';
import { useCancelCloudSubscription } from '../../Subscriptions/cloud/useCancelCloudSubscription';
import { T } from '@tolgee/react';
import { Box, Link } from '@mui/material';
import { ReachingTheLimitMessage } from './ReachingTheLimitMessage';

export const TrialAlertPlanAutoRenewsContent: FC<
  SubscriptionsTrialAlertProps
> = (props) => {
  const formatDate = useDateFormatter();

  const { doCancel } = useCancelCloudSubscription();

  return (
    <>
      <T
        keyName="billing-subscription-auto-renews-alert"
        params={{ trialEnd: formatDate(props.subscription.trialEnd), b: <b /> }}
      />
      <Box mt={2}>
        <T
          keyName="billing-subscription-auto-renews-alert-cancel-message"
          params={{
            link: (
              <Link
                data-cy="billing-subscription-auto-renews-alert-cancel-button"
                onClick={doCancel}
                sx={{ cursor: 'pointer' }}
              />
            ),
          }}
        />
      </Box>
      <ReachingTheLimitMessage {...props}></ReachingTheLimitMessage>
    </>
  );
};
