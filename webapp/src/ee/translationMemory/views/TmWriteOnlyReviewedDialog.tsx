import { useState } from 'react';
import {
  Box,
  Dialog,
  DialogContent,
  DialogTitle,
  FormControlLabel,
  Switch,
  Typography,
} from '@mui/material';
import { T, useTranslate } from '@tolgee/react';
import { DialogCancelSaveActions } from 'tg.ee.module/translationMemory/components/DialogCancelSaveActions';

type Props = {
  open: boolean;
  initialWriteOnlyReviewed: boolean;
  saving: boolean;
  onSave: (writeOnlyReviewed: boolean) => void;
  onClose: () => void;
};

/**
 * Focused settings dialog containing just the `writeOnlyReviewed` toggle. Used wherever
 * the full TM settings dialog doesn't apply (project TMs from the project-settings page,
 * project TMs from the org TM list). Parent owns the save call — this component is pure UI.
 */
export const TmWriteOnlyReviewedDialog: React.VFC<Props> = ({
  open,
  initialWriteOnlyReviewed,
  saving,
  onSave,
  onClose,
}) => {
  const { t } = useTranslate();
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
          keyName="tm_write_only_reviewed_dialog_title"
          defaultValue="TM settings"
        />
      </DialogTitle>
      <DialogContent>
        <Box>
          <FormControlLabel
            control={
              <Switch
                checked={writeOnlyReviewed}
                onChange={(_, v) => setWriteOnlyReviewed(v)}
                data-cy="tm-write-only-reviewed-toggle"
              />
            }
            label={t(
              'project_tm_only_include_reviewed_label',
              'Only include reviewed translations'
            )}
          />
          <Typography variant="caption" color="text.secondary" display="block">
            <T
              keyName="project_tm_only_include_reviewed_hint"
              defaultValue="Only translations in the Reviewed state are offered as TM suggestions. Other translations are excluded until they are reviewed."
            />
          </Typography>
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
