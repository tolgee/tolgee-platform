import React, { FC } from 'react';
import { useDateFormatter } from 'tg.hooks/useLocale';
import { Box, Button, Typography } from '@mui/material';
import { PlanPublicChip } from '../../component/Plan/PlanPublicChip';
import { T } from '@tolgee/react';
import { components } from 'tg.service/billingApiSchema.generated';

type Props = {
  item: components['schemas']['OrganizationWithSubscriptionsModel'];
  onOpenAssignTrialDialog: () => void;
};

export const AdministrationSubscriptionsCloudSubscriptionPopover: FC<Props> = ({
  item,
  onOpenAssignTrialDialog,
}) => {
  const formatDate = useDateFormatter();

  return (
    <Box>
      <Box sx={{ display: 'flex', alignItems: 'center' }}>
        <Typography variant={'h3'} sx={{ display: 'inline' }}>
          {item.cloudSubscription?.plan.name}
        </Typography>
        <Box ml={1}></Box>
        <PlanPublicChip isPublic={item.cloudSubscription?.plan.public} />
      </Box>
      <Box>
        <Typography variant={'body2'}>
          {item.cloudSubscription?.currentBillingPeriod}
        </Typography>
        <Typography variant={'body2'}>
          {item.cloudSubscription?.currentPeriodEnd &&
            formatDate(item.cloudSubscription?.currentPeriodEnd)}
        </Typography>
        <Button color="primary" onClick={() => onOpenAssignTrialDialog()}>
          <T keyName="administration-subscriptions-assign-trial" />
        </Button>
      </Box>
    </Box>
  );
};
