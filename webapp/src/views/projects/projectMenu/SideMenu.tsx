import clsx from 'clsx';
import React from 'react';
import Drawer from '@material-ui/core/Drawer';
import { makeStyles } from '@material-ui/core/styles';

import { ToggleButton } from './ToggleButton';

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
  toggleRight: {
    display: 'flex',
    justifyContent: 'flex-end',
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
        <div className={classes.toggleRight}>
          <ToggleButton onClick={onSideMenuToggle} open={open} />
        </div>
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
