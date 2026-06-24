import { useState } from 'react';
import { Box, Dialog, DialogContent, DialogTitle } from '@mui/material';
import { T } from '@tolgee/react';
import { WriteOnlyReviewedSwitch } from 'tg.ee.module/translationMemory/components/form/fields/WriteOnlyReviewedSwitch';
import { DialogCancelSaveActions } from 'tg.component/common/dialog/DialogCancelSaveActions';

type Props = {
  open: boolean;
  initialWriteOnlyReviewed: boolean;
  saving: boolean;
  onSave: (writeOnlyReviewed: boolean) => void;
  onClose: () => void;
};

/**
 * Focused settings dialog containing just the `writeOnlyReviewed` toggle. Used by the
 * content view's gear icon for PROJECT TMs (the full settings dialog doesn't apply there).
 * Parent owns the save call — this component is pure UI.
 */
export const TmWriteOnlyReviewedDialog: React.VFC<Props> = ({
  open,
  initialWriteOnlyReviewed,
  saving,
  onSave,
  onClose,
}) => {
  const [writeOnlyReviewed, setWriteOnlyReviewed] = useState(
    initialWriteOnlyReviewed
  );

  return (
    <Dialog
      open={open}
      onClose={onClose}
      maxWidth="sm"
      fullWidth
      data-cy="tm-write-only-reviewed-dialog"
    >
      <DialogTitle>
        <T
          keyName="translation_memory_settings_title"
          defaultValue="TM settings"
        />
      </DialogTitle>
      <DialogContent>
        <Box>
          <WriteOnlyReviewedSwitch
            checked={writeOnlyReviewed}
            onChange={setWriteOnlyReviewed}
            switchDataCy="tm-write-only-reviewed-toggle"
          />
        </Box>
      </DialogContent>
      <DialogCancelSaveActions
        onCancel={onClose}
        onSave={() => onSave(writeOnlyReviewed)}
        saving={saving}
        saveDataCy="tm-write-only-reviewed-save"
      />
    </Dialog>
  );
};
