import React, { PropsWithChildren } from 'react';
import { Button } from '@mui/material';
import { Link, useRouteMatch } from 'react-router-dom';

type ButtonGroupRouterItemProps = PropsWithChildren<{
  link: string;
  exact?: boolean;
}>;

export const ButtonGroupRouterItem = (props: ButtonGroupRouterItemProps) => {
  const active = !!useRouteMatch({ path: props.link, exact: props.exact })
    ?.path;

  return (
    <Button
      component={Link}
      to={props.link}
      size="small"
      disableElevation
      color={active ? 'primary' : 'default'}
      data-cy="billing-subscriptions-cloud-button"
    >
      {props.children}
    </Button>
  );
};
