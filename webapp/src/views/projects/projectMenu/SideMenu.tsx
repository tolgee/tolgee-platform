import clsx from 'clsx';
import React, { useEffect } from 'react';
import Drawer from '@material-ui/core/Drawer';
import { makeStyles } from '@material-ui/core/styles';
import { useTheme } from '@material-ui/core';

import { ToggleButton } from './ToggleButton';
import { useBottomPanel } from 'tg.component/bottomPanel/BottomPanelContext';

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
    // @ts-ignore
    paddingBottom: ({ paddingBottom }) => paddingBottom,
    overflowY: 'auto',
    overflowX: 'hidden',
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
  const { height } = useBottomPanel();
  const classes = useStyles({ paddingBottom: height + 70 });
  const theme = useTheme();

  useEffect(() => {
    // trigger resize to recalculate Translations smoothly
    let loop = true;
    const makeStep = () => {
      window.dispatchEvent(new Event('resize'));
      if (loop) requestAnimationFrame(makeStep);
    };
    const timer = setTimeout(() => {
      loop = false;
    }, theme.transitions.duration.enteringScreen * 2);
    makeStep();
    return () => {
      clearTimeout(timer);
      loop = false;
    };
  }, [open]);

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
