import React, { FC } from 'react';
import { Box, Button } from '@mui/material';
import { T } from '@tolgee/react';
import { components } from 'tg.service/billingApiSchema.generated';
import { Link } from 'react-router-dom';
import { LINKS } from 'tg.constants/links';
import { OrganizationCloudCustomPlans } from './OrganizationCloudCustomPlans';
import { AssignPlanButton } from './AssignPlanButton';
import { SubscriptionsDetailPopover } from '../generic/SubscriptionsDetailPopover';
import { SubscriptionCurrentPlanInfo } from '../generic/SubscriptionCurrentPlanInfo';
import { SubscriptionsCloudEditPlanButton } from './SubscriptionsCloudEditPlanButton';

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
  return (
    <SubscriptionsDetailPopover
      popoverContent={
        <>
          <SubscriptionCurrentPlanInfo
            subscription={item.cloudSubscription}
            editButton={<SubscriptionsCloudEditPlanButton item={item} />}
          />
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
        </>
      }
    >
      {children}
    </SubscriptionsDetailPopover>
  );
};
