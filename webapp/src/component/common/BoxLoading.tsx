import Box from '@material-ui/core/Box';
import CircularProgress from '@material-ui/core/CircularProgress';
import { default as React } from 'react';

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
