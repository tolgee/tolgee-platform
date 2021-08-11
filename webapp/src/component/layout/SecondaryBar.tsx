import React, { FunctionComponent } from 'react';
import { Box } from '@material-ui/core';
import grey from '@material-ui/core/colors/grey';

export const SecondaryBar: FunctionComponent<React.ComponentProps<typeof Box>> =
  (props) => (
    <Box
      style={{
        backgroundColor: grey[50],
        borderBottom: `1px solid ${grey[200]}`,
      }}
      p={4}
      pb={2}
      pt={2}
      {...props}
    >
      {props.children}
    </Box>
  );
