import { FC, useState } from 'react';
import {
  Box,
  Dialog,
  DialogContent,
  IconButton,
  Tooltip,
  Typography,
} from '@mui/material';
import { T } from '@tolgee/react';
import { Key } from '@mui/icons-material';
import { ClipboardCopyInput } from 'tg.component/common/ClipboardCopyInput';

export const PlanLicenseKey: FC<{ licenseKey?: string }> = ({ licenseKey }) => {
  const [open, setOpen] = useState(false);

  if (!licenseKey) {
    return null;
  }

  return (
    <>
      <Tooltip title={<T keyName="active-plan-license-key-tooltip" />}>
        <IconButton
          onClick={() => setOpen(true)}
          sx={{ marginTop: '-8px', marginBottom: '-8px' }}
        >
          <Key />
        </IconButton>
      </Tooltip>
      <Dialog open={open} onClose={() => setOpen(false)} maxWidth="md">
        <DialogContent>
          <Box mb={2}>
            <Typography variant="body2">
              <T keyName="active-plan-license-key-caption" />
            </Typography>
          </Box>
          <ClipboardCopyInput value={licenseKey} />
        </DialogContent>
      </Dialog>
    </>
  );
};
