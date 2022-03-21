import React from 'react';
import ListItemIcon from '@mui/material/ListItemIcon';
import ListItemText from '@mui/material/ListItemText';
import makeStyles from '@mui/styles/makeStyles';
import { useLocation } from 'react-router-dom';
import { Theme } from '@mui/material';

import { ListItemLink } from 'tg.component/common/list/ListItemLink';

interface SideMenuItemProps {
  linkTo?: string;
  icon: React.ReactElement;
  text: string;
  selected?: boolean;
  matchAsPrefix?: boolean;
}

const useStyles = makeStyles<Theme>({
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
