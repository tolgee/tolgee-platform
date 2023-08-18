import { default as React } from 'react';
import Box from '@mui/material/Box';
import { CircularProgress } from 'tg.component/CircularProgress';

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
