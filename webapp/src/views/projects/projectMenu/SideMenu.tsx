import { Divider, ListItem, ListItemIcon } from '@material-ui/core';
import Drawer from '@material-ui/core/Drawer';
import { makeStyles } from '@material-ui/core/styles';
import ChevronLeftIcon from '@material-ui/icons/ChevronLeft';
import ChevronRightIcon from '@material-ui/icons/ChevronRight';
import clsx from 'clsx';
import React from 'react';

const drawerWidth = 240;

const useStyles = makeStyles((theme) => ({
  toolbarIcon: {
    display: 'flex',
    alignItems: 'center',
    justifyContent: 'space-between',
    flexDirection: 'row-reverse',
    padding: '0 8px',
    ...theme.mixins.toolbar,
  },
  drawerPaper: {
    position: 'relative',
    whiteSpace: 'nowrap',
    width: drawerWidth,
    transition: theme.transitions.create('width', {
      easing: theme.transitions.easing.sharp,
      duration: theme.transitions.duration.enteringScreen,
    }),
  },
  drawerPaperClose: {
    overflowX: 'hidden',
    transition: theme.transitions.create('width', {
      easing: theme.transitions.easing.sharp,
      duration: theme.transitions.duration.leavingScreen,
    }),
    width: theme.spacing(7),
  },
}));

interface SideMenuProps {
  onSideMenuToggle: () => void;
  open: boolean;
}

export const SideMenu: React.FC<SideMenuProps> = ({
  onSideMenuToggle,
  open,
  children,
}) => {
  const classes = useStyles({});

  return (
    <Drawer
      variant="permanent"
      classes={{
        paper: clsx(classes.drawerPaper, !open && classes.drawerPaperClose),
      }}
      open={open}
      color="secondary"
    >
      {children}
      <Divider />
      <ListItem button onClick={onSideMenuToggle}>
        <ListItemIcon>
          {open ? <ChevronLeftIcon /> : <ChevronRightIcon />}
        </ListItemIcon>
      </ListItem>
    </Drawer>
  );
};
