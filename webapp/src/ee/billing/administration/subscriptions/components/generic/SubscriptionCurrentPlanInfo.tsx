import React, { FC, ReactElement } from 'react';
import { Box, Button, Chip, Typography } from '@mui/material';
import { PlanPublicChip } from '../../../../component/Plan/PlanPublicChip';
import { T } from '@tolgee/react';
import { SubscriptionPeriodInfo } from '../cloudPlan/SubscriptionPerodInfo';
import { useDateFormatter } from 'tg.hooks/useLocale';
import { components } from 'tg.service/apiSchema.generated';

type SubscriptionCurrentPlanInfoProps = {
  subscription?: {
    plan: {
      public: boolean;
      name: string;
    };
    status: components['schemas']['PublicCloudSubscriptionModel']['status'];
    currentBillingPeriod?: string;
    currentPeriodStart?: number;
    currentPeriodEnd?: number;
    stripeSubscriptionId?: string;
    trialEnd?: number;
  };
  editButton: ReactElement;
  cancelButton?: ReactElement;
};

export const SubscriptionCurrentPlanInfo: FC<
  SubscriptionCurrentPlanInfoProps
> = (props) => {
  const formatDate = useDateFormatter();

  if (!props.subscription) {
    return null;
  }

  const isTrial = Boolean(props.subscription.status === 'TRIALING');

  return (
    <Box data-cy="administration-subscriptions-current-plan-info" mb={2}>
      <Box sx={{ display: 'flex', alignItems: 'center', mb: 2 }}>
        <Typography
          variant={'h3'}
          sx={{ display: 'inline', fontSize: '20px' }}
          data-cy="subscriptions-cloud-popover-active-plan-name"
        >
          {props.subscription.plan.name}{' '}
        </Typography>
        {props.editButton}
        {props.cancelButton}
        <Box ml={1}></Box>
        <PlanPublicChip isPublic={props.subscription.plan.public} />
      </Box>
      <Box>
        {isTrial && (
          <>
            <Chip
              data-cy="administration-billing-trial-badge"
              color="secondary"
              size="small"
              label={<T keyName="admin_billing_trial_badge" />}
              sx={{ mr: 1 }}
            />
            <T
              keyName="admin_billing_trial_end_message"
              params={{
                date: formatDate(props.subscription.trialEnd, {
                  dateStyle: 'short',
                  timeStyle: 'short',
                }),
              }}
            />
          </>
        )}
      </Box>
      <SubscriptionPeriodInfo
        isTrial={isTrial}
        currentBillingPeriod={props.subscription.currentBillingPeriod}
        currentPeriodStart={props.subscription.currentPeriodStart}
        currentPeriodEnd={props.subscription.currentPeriodEnd}
      />
      {props.subscription.stripeSubscriptionId && (
        <Button
          sx={{ mt: 1 }}
          color={'secondary'}
          component="a"
          href={`https://dashboard.stripe.com/subscriptions/${props.subscription.stripeSubscriptionId}`}
          target="_blank"
          size={'small'}
        >
          <T keyName="admin_billing_cloud_subscription_view_in_stripe" />
        </Button>
      )}
    </Box>
  );
};
