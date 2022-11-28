import React, { FC, PropsWithChildren } from 'react';
import { Button } from '@mui/material';
import { Link, useRouteMatch } from 'react-router-dom';

type ButtonGroupRouterItemProps = PropsWithChildren<
  {
    link: string;
    exact?: boolean;
  } & React.ComponentProps<typeof Button>
>;

export const ButtonGroupRouterItem: FC<ButtonGroupRouterItemProps> = ({
  link,
  exact,
  ...props
}) => {
  const active = !!useRouteMatch({ path: link, exact })?.path;

  return (
    <Button
      component={Link}
      to={link}
      size="small"
      disableElevation
      color={active ? 'primary' : 'default'}
      {...(props as any)}
    />
  );
};
