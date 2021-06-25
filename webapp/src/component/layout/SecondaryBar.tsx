import { Box } from '@material-ui/core';
import grey from '@material-ui/core/colors/grey';
import { FunctionComponent } from 'react';

export const SecondaryBar: FunctionComponent = (props) => (
  <Box
    style={{
      backgroundColor: grey[50],
      borderBottom: `1px solid ${grey[200]}`,
    }}
    p={4}
    pb={2}
    pt={2}
  >
    {props.children}
  </Box>
);
