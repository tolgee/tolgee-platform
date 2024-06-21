import { useEffect, useState } from 'react';
import {
  Button,
  Dialog,
  DialogActions,
  DialogContent,
  DialogTitle,
} from '@mui/material';
import { T } from '@tolgee/react';
import { ClipboardCopyInput } from 'tg.component/common/ClipboardCopyInput';

type Props = {
  licenseKey?: string;
  defaultOpen: boolean;
  custom?: boolean;
};

export const PlanLicenseKey = ({ licenseKey, defaultOpen, custom }: Props) => {
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
      <Button
        onClick={() => setOpen(true)}
        size="small"
        color={custom ? 'info' : 'primary'}
        variant="contained"
      >
        <T keyName="active-plan-license-key-button" />
      </Button>
      <Dialog open={open} onClose={() => setOpen(false)} maxWidth="md">
        <DialogTitle>
          <T keyName="active-plan-license-key-caption" />
        </DialogTitle>
        <DialogContent>
          <ClipboardCopyInput
            value={licenseKey}
            inputProps={{
              style: { width: 420 },
              'data-cy': 'active-plan-license-key-input',
            }}
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
