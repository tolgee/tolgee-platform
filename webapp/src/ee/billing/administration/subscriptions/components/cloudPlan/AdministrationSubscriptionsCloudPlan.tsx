import React, { FC, useState } from 'react';
import { components } from 'tg.service/billingApiSchema.generated';
import { SubscriptionCloudPlanPopover } from './SubscriptionCloudPlanPopover';
import { AssignCloudPlanDialog } from './AssignCloudPlanDialog';
import { T } from '@tolgee/react';
import { SubscriptionRowPlanInfo } from '../generic/SubscriptionRowPlanInfo';
import { Box } from '@mui/material';

type Props = {
  item: components['schemas']['OrganizationWithSubscriptionsModel'];
};

export const AdministrationSubscriptionsCloudPlan: FC<Props> = ({ item }) => {
  const [assignTrialDialogOpen, setAssignTrialDialogOpen] = useState(false);

  return (
    <>
      <SubscriptionCloudPlanPopover
        item={item}
        onOpenAssignTrialDialog={() => setAssignTrialDialogOpen(true)}
      >
        <Box>
          <SubscriptionRowPlanInfo
            dataCy={'administration-subscriptions-cloud-plan-name'}
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
      <AssignCloudPlanDialog
        organizationId={item.organization.id}
        open={assignTrialDialogOpen}
        handleClose={() => setAssignTrialDialogOpen(false)}
        item={item}
      />
    </>
  );
};
