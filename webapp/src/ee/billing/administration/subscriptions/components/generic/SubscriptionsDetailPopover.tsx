import React from 'react';
import { Box, Tooltip } from '@mui/material';

type SubscriptionsDetailPopoverProps = {
  children: React.ReactElement;
  popoverContent: React.ReactElement;
};

export const SubscriptionsDetailPopover = ({
  children,
  popoverContent,
}: SubscriptionsDetailPopoverProps) => {
  return (
    <Tooltip
      componentsProps={{ tooltip: { sx: { maxWidth: 'none' } } }}
      placement="bottom-start"
      title={
        <Box sx={{ p: 1 }} data-cy="administration-subscriptions-plan-popover">
          {popoverContent}
        </Box>
      }
    >
      {children}
    </Tooltip>
  );
};
