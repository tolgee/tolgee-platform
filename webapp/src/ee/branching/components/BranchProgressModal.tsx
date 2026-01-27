import React, { FC } from 'react';
import {
  Box,
  Dialog,
  DialogContent,
  DialogTitle,
  LinearProgress,
  Typography,
} from '@mui/material';
import { T } from '@tolgee/react';

export const BranchProgressModal: FC<{
  open: boolean;
  title?: React.ReactNode;
  message?: React.ReactNode;
}> = ({ open, title, message }) => {
  return (
    <Dialog open={open} disableEscapeKeyDown>
      <DialogTitle>
        {title || <T keyName="branch_operation_in_progress" />}
      </DialogTitle>
      <DialogContent sx={{ width: 400, maxWidth: '100%', py: 3 }}>
        <Box display="flex" flexDirection="column" alignItems="center" gap={2}>
          <LinearProgress sx={{ width: '100%' }} />
          {message && (
            <Typography
              variant="body2"
              color="textSecondary"
              textAlign="center"
            >
              {message}
            </Typography>
          )}
        </Box>
      </DialogContent>
    </Dialog>
  );
};
