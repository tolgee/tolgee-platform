import { Button } from '@mui/material';
import { T } from '@tolgee/react';
import React from 'react';

export const SubscriptionsPopoverAssignPlanButton = (props: {
  onClick: () => void;
}) => {
  return (
    <Button
      color="primary"
      onClick={props.onClick}
      data-cy="administration-assign-plan-button"
    >
      <T keyName="administration-subscriptions-assign-plan" />
    </Button>
  );
};
