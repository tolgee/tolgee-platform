import { makeStyles, MenuItem, ListSubheader } from '@material-ui/core';
import React from 'react';

const useStyles = makeStyles((theme) => ({
  compactItem: {
    height: '40px',
  },
  listSubheader: {
    lineHeight: 'unset',
    paddingTop: theme.spacing(2),
    paddingBottom: theme.spacing(0.5),
  },
}));

export const CompactMenuItem: React.FC<React.ComponentProps<typeof MenuItem>> =
  (props) => {
    const classes = useStyles();
    return <MenuItem className={classes.compactItem} {...props} />;
  };

export const CompactListSubheader: React.FC<
  React.ComponentProps<typeof ListSubheader>
> = (props) => {
  const classes = useStyles();
  return <ListSubheader className={classes.listSubheader} {...props} />;
};
