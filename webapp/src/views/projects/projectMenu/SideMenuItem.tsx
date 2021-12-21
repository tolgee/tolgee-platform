import React from 'react';
import ListItemIcon from '@material-ui/core/ListItemIcon';
import ListItemText from '@material-ui/core/ListItemText';
import { makeStyles } from '@material-ui/core';
import { useLocation } from 'react-router-dom';

import { ListItemLink } from 'tg.component/common/list/ListItemLink';

interface SideMenuItemProps {
  linkTo?: string;
  icon: React.ReactElement;
  text: string;
  selected?: boolean;
  matchAsPrefix?: boolean;
}

const useStyles = makeStyles({
  item: {
    '& > span': {
      textOverflow: 'ellipsis',
      whiteSpace: 'nowrap',
      overflow: 'hidden',
      marginRight: -10,
    },
  },
});

export function SideMenuItem({
  linkTo,
  icon,
  text,
  selected,
  matchAsPrefix,
}: SideMenuItemProps) {
  const match = useLocation();
  const classes = useStyles();

  const isSelected = selected
    ? true
    : matchAsPrefix
    ? match.pathname.startsWith(String(linkTo))
    : match.pathname === linkTo;

  return (
    <ListItemLink selected={isSelected} to={linkTo || ''}>
      <ListItemIcon>{icon}</ListItemIcon>
      <ListItemText className={classes.item} primary={text} />
    </ListItemLink>
  );
}
