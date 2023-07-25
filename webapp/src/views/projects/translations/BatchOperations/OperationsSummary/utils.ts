import { useTheme } from '@mui/material';

import { BatchJobStatus } from '../types';

export function useStatusColor() {
  const { palette } = useTheme();

  return (status: BatchJobStatus) => {
    switch (status) {
      case 'FAILED':
        return palette.error.main;
      case 'SUCCESS':
        return palette.success.main;
      default:
        return palette.text.secondary;
    }
  };
}

export const END_STATUSES: BatchJobStatus[] = [
  'SUCCESS',
  'FAILED',
  'CANCELLED',
];

export const STATIC_STATUSES: BatchJobStatus[] = [
  'FAILED',
  'SUCCESS',
  'CANCELLED',
  'PENDING',
];

export const CANCELLABLE_STATUSES: BatchJobStatus[] = ['PENDING', 'RUNNING'];
