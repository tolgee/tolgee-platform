import React, { FC } from 'react';
import { Box } from '@mui/material';
import { components } from 'tg.service/billingApiSchema.generated';
import { LINKS } from 'tg.constants/links';
import { SubscriptionsPopoverCloudCustomPlans } from './SubscriptionsPopoverCloudCustomPlans';
import { SubscriptionsPopoverAssignPlanButton } from '../generic/assignPlan/SubscriptionsPopoverAssignPlanButton';
import { SubscriptionsDetailPopover } from '../generic/SubscriptionsDetailPopover';
import { SubscriptionCurrentPlanInfo } from '../generic/SubscriptionCurrentPlanInfo';
import { SubscriptionsCloudEditPlanButton } from './SubscriptionsCloudEditPlanButton';
import { SubscriptionsPopoverCreateCustomPlanButton } from '../generic/SubscriptionsPopoverCreateCustomPlanButton';
import { SubscriptionCloudCancelPlanButton } from './SubscriptionCloudCancelPlanButton';

type Props = {
  item: components['schemas']['OrganizationWithSubscriptionsModel'];
  onOpenAssignPlanDialog: () => void;
  children: React.ReactElement;
};

export const SubscriptionCloudPlanPopover: FC<Props> = ({
  item,
  onOpenAssignPlanDialog,
  children,
}) => {
  return (
    <SubscriptionsDetailPopover
      popoverContent={
        <>
          <SubscriptionCurrentPlanInfo
            subscription={item.cloudSubscription}
            editButton={<SubscriptionsCloudEditPlanButton item={item} />}
            cancelButton={<SubscriptionCloudCancelPlanButton item={item} />}
          />
          <SubscriptionsPopoverCloudCustomPlans item={item} />
          <Box display="flex" mt={3}>
            <SubscriptionsPopoverAssignPlanButton
              onClick={() => onOpenAssignPlanDialog()}
            />
            <SubscriptionsPopoverCreateCustomPlanButton
              link={
                LINKS.ADMINISTRATION_BILLING_CLOUD_PLAN_CREATE.build() +
                '?creatingForOrganizationId=' +
                item.organization.id
              }
            />
          </Box>
        </>
      }
    >
      {children}
    </SubscriptionsDetailPopover>
  );
};
