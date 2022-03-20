import React, { FunctionComponent } from 'react';
import { Box } from '@material-ui/core';

export const SecondaryBar: FunctionComponent<React.ComponentProps<typeof Box>> =
  (props) => (
    <Box
      sx={{
        boxShadow: 1, // theme.shadows[1]
        color: 'primary.main', // theme.palette.primary.main
        m: 1, // margin: theme.spacing(1)
        p: {
          xs: 1, // [theme.breakpoints.up('xs')]: { padding: theme.spacing(1) }
        },
        zIndex: 'tooltip', // theme.zIndex.tooltip
      }}
      p={4}
      pb={2}
      pt={2}
      {...props}
    >
      {props.children}
    </Box>
  );
