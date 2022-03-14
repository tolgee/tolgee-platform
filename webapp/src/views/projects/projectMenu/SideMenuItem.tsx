import React from 'react';
import { Link, useLocation } from 'react-router-dom';
import { makeStyles, Tooltip } from '@material-ui/core';
import clsx from 'clsx';

interface SideMenuItemProps {
  linkTo?: string;
  icon: React.ReactElement;
  text: string;
  selected?: boolean;
  matchAsPrefix?: boolean;
  hidden?: boolean;
}

const useStyles = makeStyles((theme) => ({
  item: {
    display: 'flex',
    listStyle: 'none',
    flexDirection: 'column',
    alignItems: 'center',
  },
  link: {
    display: 'flex',
    padding: '10px 0px',
    cursor: 'pointer',
    justifyContent: 'center',
    color: theme.palette.grey[600],
    outline: 0,
    transition: 'all 0.2s ease-in-out',
    '&:focus, &:hover': {
      color: theme.palette.grey[800],
    },
    width: 44,
    borderRadius: 10,
  },
  selected: {
    color: theme.palette.primary.main + ' !important',
    background: theme.palette.grey[200] + ' !important',
  },
  tooltip: {
    margin: theme.spacing(0, 0.5),
  },
}));

export function SideMenuItem({
  linkTo,
  icon,
  text,
  selected,
  matchAsPrefix,
  hidden,
}: SideMenuItemProps) {
  const match = useLocation();
  const classes = useStyles();

  const isSelected = selected
    ? true
    : matchAsPrefix
    ? match.pathname.startsWith(String(linkTo))
    : match.pathname === linkTo;

  return (
    <li className={classes.item}>
      <Tooltip
        title={text}
        placement="right"
        classes={{ tooltip: classes.tooltip }}
      >
        <Link
          aria-label={text}
          to={linkTo as string}
          tabIndex={hidden ? -1 : undefined}
          className={clsx(classes.link, { [classes.selected]: isSelected })}
        >
          {icon}
        </Link>
      </Tooltip>
    </li>
  );
}
