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
  matchAsPrefix?: boolean;
}

export function SideMenuItem({
  linkTo,
  icon,
  text,
  selected,
  matchAsPrefix,
}: SideMenuItemProps) {
  const match = useLocation();

  const isSelected = selected
    ? true
    : matchAsPrefix
    ? match.pathname.startsWith(String(linkTo))
    : match.pathname === linkTo;

  return (
    <ListItemLink selected={isSelected} to={linkTo || ''}>
      <ListItemIcon>{icon}</ListItemIcon>
      <ListItemText primary={text} />
    </ListItemLink>
  );
}
