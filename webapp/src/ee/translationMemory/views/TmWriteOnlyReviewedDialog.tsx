import { useState } from 'react';
import {
  Box,
  Button,
  Dialog,
  DialogActions,
  DialogContent,
  DialogTitle,
  FormControlLabel,
  Switch,
  Typography,
} from '@mui/material';
import { T, useTranslate } from '@tolgee/react';
import LoadingButton from 'tg.component/common/form/LoadingButton';

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
      <DialogActions>
        <Button onClick={onClose}>
          <T keyName="global_cancel_button" defaultValue="Cancel" />
        </Button>
        <LoadingButton
          onClick={() => onSave(writeOnlyReviewed)}
          variant="contained"
          color="primary"
          loading={saving}
          data-cy="tm-write-only-reviewed-save"
        >
          <T keyName="global_form_save" defaultValue="Save" />
        </LoadingButton>
      </DialogActions>
    </Dialog>
  );
};
