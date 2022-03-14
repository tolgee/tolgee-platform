import React from 'react';
import clsx from 'clsx';
import { makeStyles } from '@material-ui/core';

const MENU_WIDTH = 60;

const useStyles = makeStyles((theme) => ({
  menuFixed: {
    position: 'fixed',
    top: 0,
    bottom: 0,
    overscrollBehavior: 'contain',
    margin: 0,
    padding: 0,
    width: MENU_WIDTH,
    display: 'flex',
    flexDirection: 'column',
  },
  menuWrapper: {
    minWidth: MENU_WIDTH,
    // background: theme.palette.grey[100],
  },
}));

export const SideMenu: React.FC = ({ children }) => {
  const classes = useStyles({});

  return (
    <div className={classes.menuWrapper}>
      <menu
        className={clsx(classes.menuFixed)}
        color="secondary"
        data-cy="project-menu-items"
      >
        {children}
      </menu>
    </div>
  );
};
