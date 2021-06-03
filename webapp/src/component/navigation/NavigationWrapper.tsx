import React from 'react';
import grey from '@material-ui/core/colors/grey';
import { Box, makeStyles } from '@material-ui/core';

const useStyles = makeStyles({
  container: {
    height: 48,
    display: 'flex',
    alignItems: 'center',
    '& > * + *': {
      marginLeft: 10,
    },
  },
});

export const NavigationWrapper: React.FC = ({ children }) => {
  const classes = useStyles();
  return (
    <Box
      style={{
        borderBottom: `1px solid ${grey[200]}`,
        backgroundColor: grey[50],
      }}
      pl={4}
      pr={4}
    >
      <div className={classes.container}>{children}</div>
    </Box>
  );
};
