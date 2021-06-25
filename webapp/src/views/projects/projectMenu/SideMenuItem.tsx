import ListItemIcon from '@material-ui/core/ListItemIcon';
import ListItemText from '@material-ui/core/ListItemText';
import React from 'react';
import { useRouteMatch } from 'react-router-dom';
import { ListItemLink } from 'tg.component/common/list/ListItemLink';

interface SideMenuItemProps {
  linkTo?: string;
  icon: React.ReactElement;
  text: string;
  selected?: boolean;
}

export function SideMenuItem({
  linkTo,
  icon,
  text,
  selected,
}: SideMenuItemProps) {
  const match = useRouteMatch();

  return (
    <ListItemLink
      selected={selected ? true : match.url === linkTo}
      to={linkTo || ''}
    >
      <ListItemIcon>{icon}</ListItemIcon>
      <ListItemText primary={text} />
    </ListItemLink>
  );
}
