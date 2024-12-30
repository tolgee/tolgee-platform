import { Box, styled, Tooltip } from '@mui/material';
import React, { FC, useState } from 'react';
import { components } from 'tg.service/billingApiSchema.generated';
import { SubscriptionCloudPlanPopover } from './SubscriptionCloudPlanPopover';
import { AssignCloudTrialDialog } from './AssignCloudTrialDialog';

type Props = {
  item: components['schemas']['OrganizationWithSubscriptionsModel'];
};

const StyledContainer = styled(Box)`
  display: inline-block;
  margin-left: 4px;
`;

export const AdministrationSubscriptionsCloudPlan: FC<Props> = ({ item }) => {
  const [dialogOpen, setDialogOpen] = useState(false);
  return (
    <>
      <SubscriptionCloudPlanPopover
        item={item}
        onOpenAssignTrialDialog={() => setDialogOpen(true)}
      >
        <StyledContainer>{item.cloudSubscription?.plan.name}</StyledContainer>
      </SubscriptionCloudPlanPopover>
      <AssignCloudTrialDialog
        organizationId={item.organization.id}
        open={dialogOpen}
        handleClose={() => setDialogOpen(false)}
      />
    </>
  );
};
