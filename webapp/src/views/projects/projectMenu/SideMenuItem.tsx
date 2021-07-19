import React from 'react';
import ListItemIcon from '@material-ui/core/ListItemIcon';
import ListItemText from '@material-ui/core/ListItemText';
import { useLocation } from 'react-router-dom';

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
  const match = useLocation();

  return (
    <ListItemLink
      selected={selected ? true : match.pathname === linkTo}
      to={linkTo || ''}
    >
      <ListItemIcon>{icon}</ListItemIcon>
      <ListItemText primary={text} />
    </ListItemLink>
  );
}
