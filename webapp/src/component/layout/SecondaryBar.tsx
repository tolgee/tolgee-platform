import React, { FunctionComponent } from 'react';
import { Box } from '@material-ui/core';
import { grey } from '@material-ui/core/colors';

export const SecondaryBar: FunctionComponent<React.ComponentProps<typeof Box>> =
  (props) => (
    <Box
      sx={{
        borderBottom: `1px solid ${grey[200]}`,
        bgcolor: 'extraLightBackground.main',
      }}
      p={4}
      pb={2}
      pt={2}
      {...props}
    >
      {props.children}
    </Box>
  );
