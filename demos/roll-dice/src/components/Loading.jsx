import * as React from 'react';
import CircularProgress from '@mui/material/CircularProgress';
import Box from '@mui/material/Box';

export default function Loading() {
  return (
    <Box sx={{ display: 'flex', height: '100dvh', width: '100%', alignItems:'center', justifyContent: 'center' }}>
      <CircularProgress />
    </Box>
  );
}