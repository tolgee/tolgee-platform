import React, { FunctionComponent } from 'react';
import { Box } from '@mui/material';
import { grey } from '@mui/material/colors';

export const SecondaryBar: FunctionComponent<React.ComponentProps<typeof Box>> =
  (props) => (
    <Box
      style={{
        borderBottom: `1px solid ${grey[200]}`,
      }}
      m={3}
      mb={0}
      mt={0}
      pb={2}
      pt={2}
      {...props}
    >
      {props.children}
    </Box>
  );
