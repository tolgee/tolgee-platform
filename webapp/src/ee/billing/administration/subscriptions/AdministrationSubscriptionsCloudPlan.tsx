import { Box, styled, Tooltip } from '@mui/material';
import React, { FC, useState } from 'react';
import { components } from 'tg.service/billingApiSchema.generated';
import { AdministrationSubscriptionsCloudSubscriptionPopover } from './AdministrationSubscriptionsCloudSubscriptionPopover';
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
      <Tooltip
        title={
          <AdministrationSubscriptionsCloudSubscriptionPopover
            item={item}
            onOpenAssignTrialDialog={() => setDialogOpen(true)}
          />
        }
      >
        <StyledContainer>{item.cloudSubscription?.plan.name}</StyledContainer>
      </Tooltip>
      <AssignCloudTrialDialog
        organizationId={item.organization.id}
        open={dialogOpen}
        handleClose={() => setDialogOpen(false)}
      ></AssignCloudTrialDialog>
    </>
  );
};
