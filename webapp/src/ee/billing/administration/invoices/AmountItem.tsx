import { FC } from 'react';
import { Box } from '@mui/material';

type AmountItemProps = {
  label: string;
  value: string;
};

export const AmountItem: FC<AmountItemProps> = ({ label, value }) => (
  <Box sx={{ textAlign: 'right' }}>
    <Box sx={{ fontSize: '0.75rem', color: 'text.secondary' }}>{label}</Box>
    <Box>{value}</Box>
  </Box>
);
