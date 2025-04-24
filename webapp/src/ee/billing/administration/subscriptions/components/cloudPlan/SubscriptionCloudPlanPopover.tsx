import React, { FC } from 'react';
import { useDateFormatter } from 'tg.hooks/useLocale';
import { Box, Button, Chip, Tooltip, Typography } from '@mui/material';
import { PlanPublicChip } from '../../../../component/Plan/PlanPublicChip';
import { T } from '@tolgee/react';
import { components } from 'tg.service/billingApiSchema.generated';
import { SubscriptionsCloudEditPlanButton } from './SubscriptionsCloudEditPlanButton';
import { Link } from 'react-router-dom';
import { LINKS } from 'tg.constants/links';
import { OrganizationCloudCustomPlans } from './OrganizationCloudCustomPlans';
import { SubscriptionPeriodInfo } from './SubscriptionPerodInfo';
import { AssignPlanButton } from './AssignPlanButton';

type Props = {
  item: components['schemas']['OrganizationWithSubscriptionsModel'];
  onOpenAssignTrialDialog: () => void;
  children: React.ReactElement;
};

export const SubscriptionCloudPlanPopover: FC<Props> = ({
  item,
  onOpenAssignTrialDialog,
  children,
}) => {
  const formatDate = useDateFormatter();

  const isTrial = Boolean(item.cloudSubscription?.status === 'TRIALING');

  return (
    <Tooltip
      componentsProps={{ tooltip: { sx: { maxWidth: 'none' } } }}
      title={
        <Box sx={{ p: 1 }} data-cy="administration-subscriptions-plan-popover">
          <Box sx={{ display: 'flex', alignItems: 'center', mb: 2 }}>
            <Typography
              variant={'h3'}
              sx={{ display: 'inline', fontSize: '20px' }}
            >
              {item.cloudSubscription?.plan.name}{' '}
            </Typography>
            <SubscriptionsCloudEditPlanButton item={item} />
            <Box ml={1}></Box>
            <PlanPublicChip isPublic={item.cloudSubscription?.plan.public} />
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
                    date: formatDate(item.cloudSubscription?.trialEnd, {
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
            currentBillingPeriod={item.cloudSubscription?.currentBillingPeriod}
            currentPeriodStart={item.cloudSubscription?.currentPeriodStart}
            currentPeriodEnd={item.cloudSubscription?.currentPeriodEnd}
          />
          {item.cloudSubscription?.stripeSubscriptionId && (
            <Button
              sx={{ mt: 1 }}
              color={'secondary'}
              component="a"
              href={`https://dashboard.stripe.com/subscriptions/${item.cloudSubscription?.stripeSubscriptionId}`}
              target="_blank"
              size={'small'}
            >
              <T keyName="admin_billing_cloud_subscription_view_in_stripe" />
            </Button>
          )}
          <OrganizationCloudCustomPlans item={item} />
          <Box mt={2}>
            <Box display="flex" mt={3}>
              <AssignPlanButton onClick={() => onOpenAssignTrialDialog()} />
              <Button
                sx={{ ml: 1 }}
                color="primary"
                component={Link}
                to={
                  LINKS.ADMINISTRATION_BILLING_CLOUD_PLAN_CREATE.build() +
                  '?creatingForOrganizationId=' +
                  item.organization.id
                }
                data-cy="administration-create-custom-plan-button"
              >
                <T keyName="administration-subscriptions-create-custom-plan" />
              </Button>
            </Box>
          </Box>
        </Box>
      }
    >
      {children}
    </Tooltip>
  );
};
