import React, { FC, useState } from 'react';
import { components } from 'tg.service/billingApiSchema.generated';
import { SubscriptionCloudPlanPopover } from './SubscriptionCloudPlanPopover';
import { AssignCloudTrialDialog } from './AssignCloudTrialDialog';
import { T } from '@tolgee/react';
import { SubscriptionRowPlanInfo } from '../generic/SubscriptionRowPlanInfo';
import { Box } from '@mui/material';
import { SubscriptionsAddPlanDialog } from './SubscriptionsAddPlanDialog';

type Props = {
  item: components['schemas']['OrganizationWithSubscriptionsModel'];
};

export const AdministrationSubscriptionsCloudPlan: FC<Props> = ({ item }) => {
  const [assignTrialDialogOpen, setAssignTrialDialogOpen] = useState(false);
  const [addPlanDialogOpen, setAddPlanDialogOpen] = useState(false);

  return (
    <>
      <SubscriptionCloudPlanPopover
        item={item}
        onOpenAssignTrialDialog={() => setAssignTrialDialogOpen(true)}
        onOpenAddPlanDialog={() => setAddPlanDialogOpen(true)}
      >
        <Box>
          <SubscriptionRowPlanInfo
            label={
              <T
                keyName={'administration-subscriptions-active-cloud-plan-cell'}
              />
            }
          >
            {item.cloudSubscription?.plan.name ?? 'N/A'}
          </SubscriptionRowPlanInfo>
        </Box>
      </SubscriptionCloudPlanPopover>
      <AssignCloudTrialDialog
        organizationId={item.organization.id}
        open={assignTrialDialogOpen}
        handleClose={() => setAssignTrialDialogOpen(false)}
      />
      <SubscriptionsAddPlanDialog
        organization={item.organization}
        open={addPlanDialogOpen}
        onClose={() => setAddPlanDialogOpen(false)}
      />
    </>
  );
};
