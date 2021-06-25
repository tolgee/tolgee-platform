import { default as React } from 'react';
import Box from '@material-ui/core/Box';
import CircularProgress from '@material-ui/core/CircularProgress';

export function BoxLoading(props: React.ComponentProps<typeof Box>) {
  return (
    <Box
      display="flex"
      alignItems="center"
      justifyContent="center"
      p={4}
      {...props}
    >
      <CircularProgress />
    </Box>
  );
}
