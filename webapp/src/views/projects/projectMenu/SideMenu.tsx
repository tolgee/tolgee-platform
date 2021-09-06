import React from 'react';
import { Divider, ListItem, ListItemIcon } from '@material-ui/core';
import Drawer from '@material-ui/core/Drawer';
import { makeStyles } from '@material-ui/core/styles';
import ChevronLeftIcon from '@material-ui/icons/ChevronLeft';
import ChevronRightIcon from '@material-ui/icons/ChevronRight';
import clsx from 'clsx';

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
    whiteSpace: 'nowrap',
    width: drawerWidth,
    transition: theme.transitions.create('width', {
      easing: theme.transitions.easing.sharp,
      duration: theme.transitions.duration.enteringScreen,
    }),
  },
  drawerFixed: {
    position: 'fixed',
    top: theme.mixins.toolbar.minHeight,
    bottom: 0,
    overscrollBehavior: 'contain',
  },
  drawerFake: {
    position: 'relative',
    visibility: 'hidden',
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
    <>
      <Drawer
        variant="permanent"
        classes={{
          paper: clsx(
            classes.drawerPaper,
            classes.drawerFixed,
            !open && classes.drawerPaperClose
          ),
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
      <Drawer
        variant="permanent"
        open={open}
        classes={{
          paper: clsx(
            classes.drawerPaper,
            classes.drawerFake,
            !open && classes.drawerPaperClose
          ),
        }}
      ></Drawer>
    </>
  );
};
