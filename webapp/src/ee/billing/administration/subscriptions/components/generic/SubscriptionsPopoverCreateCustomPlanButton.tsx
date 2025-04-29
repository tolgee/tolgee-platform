import React, { FC } from 'react';
import { Button } from '@mui/material';
import { Link } from 'react-router-dom';
import { T } from '@tolgee/react';

type SubscriptionsPopoverCreateCustomPlanButtonProps = {
  link: string;
};

export const SubscriptionsPopoverCreateCustomPlanButton: FC<
  SubscriptionsPopoverCreateCustomPlanButtonProps
> = ({ link }) => {
  return (
    <Button
      sx={{ ml: 1 }}
      color="primary"
      component={Link}
      to={link}
      data-cy="administration-create-custom-plan-button"
    >
      <T keyName="administration-subscriptions-create-custom-plan" />
    </Button>
  );
};
