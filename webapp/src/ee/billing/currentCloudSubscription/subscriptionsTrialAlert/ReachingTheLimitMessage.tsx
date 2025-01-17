import { FC } from 'react';
import { Box } from '@mui/material';
import { T } from '@tolgee/react';
import { SubscriptionsTrialAlertProps } from './SubscriptionsTrialAlert';

export const ReachingTheLimitMessage: FC<SubscriptionsTrialAlertProps> = (
  props
) => {
  if (props.subscription.plan.type !== 'PAY_AS_YOU_GO') {
    return null;
  }

  const runningOutOfMtCredits = props.usage.creditProgress > 0.9;
  const runningOutOfTranslations = props.usage.translationsProgress > 0.9;

  if (!runningOutOfMtCredits && !runningOutOfTranslations) {
    return null;
  }

  return (
    <Box mt={2} data-cy="subscriptions-trial-alert-reaching-the-limit">
      <T keyName="billing-subscription-trial-alert-reaching-the-limit-message" />
    </Box>
  );
};
