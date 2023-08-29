import { default as React } from 'react';
import Box from '@mui/material/Box';
import { SpinnerProgress } from 'tg.component/SpinnerProgress';

export function BoxLoading(props: React.ComponentProps<typeof Box>) {
  return (
    <Box
      display="flex"
      alignItems="center"
      justifyContent="center"
      p={4}
      {...props}
    >
      <SpinnerProgress />
    </Box>
  );
}
