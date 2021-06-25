import { Box, makeStyles } from '@material-ui/core';
import grey from '@material-ui/core/colors/grey';
import React from 'react';

const useStyles = makeStyles((theme) => ({
  container: {
    height: 49,
    paddingLeft: theme.spacing(4),
    borderBottom: `1px solid ${grey[200]}`,
    backgroundColor: grey[50],
    '& > * + *': {
      marginLeft: theme.spacing(2),
    },
  },
}));

export const NavigationWrapper: React.FC = ({ children }) => {
  const classes = useStyles();
  return (
    <Box display="flex" alignItems="center" className={classes.container}>
      {children}
    </Box>
  );
};
