import React, { FC } from 'react';
import { SubscriptionsDetailPopover } from '../generic/SubscriptionsDetailPopover';
import { components } from 'tg.service/billingApiSchema.generated';
import { SubscriptionCurrentPlanInfo } from '../generic/SubscriptionCurrentPlanInfo';
import { SubscriptionsSelfHostedEditPlanButton } from './SubscriptionsSelfHostedEditPlanButton';
import { SubscriptionsPopoverAssignPlanButton } from '../generic/assignPlan/SubscriptionsPopoverAssignPlanButton';
import { SubscriptionsPopoverSelfHostedCustomPlans } from './SubscriptionsPopoverSelfHostedCustomPlans';
import { Box } from '@mui/material';
import { SubscriptionsPopoverCreateCustomPlanButton } from '../generic/SubscriptionsPopoverCreateCustomPlanButton';
import { LINKS } from 'tg.constants/links';
import { SubscriptionSelfHostedCancelPlanButton } from './SubscriptionSelfHostedCancelPlanButton';

type SubscriptionsSelfHostedPopoverProps = {
  item: components['schemas']['OrganizationWithSubscriptionsModel'];
  children: React.ReactElement;
  onOpenAssignTrialDialog: () => void;
};

export const SubscriptionsSelfHostedPopover: FC<
  SubscriptionsSelfHostedPopoverProps
> = ({ item, children, onOpenAssignTrialDialog }) => {
  return (
    <SubscriptionsDetailPopover
      popoverContent={
        <>
          {item.selfHostedSubscriptions.map((subscription) => (
            <SubscriptionCurrentPlanInfo
              key={subscription.id}
              subscription={subscription}
              editButton={
                <SubscriptionsSelfHostedEditPlanButton
                  subscription={subscription}
                  organizationId={item.organization.id}
                />
              }
              cancelButton={
                <SubscriptionSelfHostedCancelPlanButton
                  subscription={subscription}
                />
              }
            />
          ))}
          <SubscriptionsPopoverSelfHostedCustomPlans item={item} />
          <Box mt={3}>
            <SubscriptionsPopoverAssignPlanButton
              onClick={onOpenAssignTrialDialog}
            />
            <SubscriptionsPopoverCreateCustomPlanButton
              link={
                LINKS.ADMINISTRATION_BILLING_EE_PLAN_CREATE.build() +
                '?creatingForOrganizationId=' +
                item.organization.id
              }
            />
          </Box>
        </>
      }
    >
      <span>{children}</span>
    </SubscriptionsDetailPopover>
  );
};
