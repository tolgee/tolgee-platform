import React, { FC, useState } from 'react';
import { components } from 'tg.service/billingApiSchema.generated';
import { SubscriptionCloudPlanPopover } from './SubscriptionCloudPlanPopover';
import { AssignCloudPlanDialog } from './assignPlan/AssignCloudPlanDialog';
import { T } from '@tolgee/react';
import { SubscriptionRowPlanInfo } from '../generic/SubscriptionRowPlanInfo';
import { Box } from '@mui/material';

type Props = {
  item: components['schemas']['OrganizationWithSubscriptionsModel'];
};

export const AdministrationSubscriptionsCloudPlan: FC<Props> = ({ item }) => {
  const [assignPlanDialogOpen, setAssignPlanDialogOpen] = useState(false);

  return (
    <>
      <SubscriptionCloudPlanPopover
        item={item}
        onOpenAssignPlanDialog={() => setAssignPlanDialogOpen(true)}
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
        open={assignPlanDialogOpen}
        onClose={() => setAssignPlanDialogOpen(false)}
        item={item}
      />
    </>
  );
};
