import { FC, useEffect, useState } from 'react';
import {
  Box,
  Button,
  Dialog,
  DialogActions,
  DialogContent,
  Typography,
} from '@mui/material';
import { T } from '@tolgee/react';
import { ClipboardCopyInput } from 'tg.component/common/ClipboardCopyInput';

type Props = {
  licenseKey?: string;
  defaultOpen: boolean;
};

export const PlanLicenseKey = ({ licenseKey, defaultOpen }: Props) => {
  useEffect(() => {
    if (defaultOpen) {
      setOpen(true);
    }
  }, [defaultOpen]);

  const [open, setOpen] = useState(false);

  if (!licenseKey) {
    return null;
  }

  return (
    <>
      <Button onClick={() => setOpen(true)} size="small">
        <T keyName="active-plan-license-key-tooltip" />
      </Button>
      <Dialog open={open} onClose={() => setOpen(false)} maxWidth="md">
        <DialogContent>
          <Box mb={2}>
            <Typography variant="body2">
              <T keyName="active-plan-license-key-caption" />
            </Typography>
          </Box>
          <ClipboardCopyInput
            value={licenseKey}
            inputProps={{ style: { width: 420 } }}
          />
        </DialogContent>
        <DialogActions>
          <Button onClick={() => setOpen(false)}>
            <T keyName="global_close_button" />
          </Button>
        </DialogActions>
      </Dialog>
    </>
  );
};
