import { Button, DialogActions } from '@mui/material';
import { T } from '@tolgee/react';
import LoadingButton from 'tg.component/common/form/LoadingButton';

type Props = {
  onCancel: () => void;
  onSave: () => void;
  saving: boolean;
  saveDataCy?: string;
};

export const DialogCancelSaveActions: React.VFC<Props> = ({
  onCancel,
  onSave,
  saving,
  saveDataCy,
}) => (
  <DialogActions>
    <Button onClick={onCancel}>
      <T keyName="global_cancel_button" defaultValue="Cancel" />
    </Button>
    <LoadingButton
      variant="contained"
      color="primary"
      onClick={onSave}
      loading={saving}
      data-cy={saveDataCy}
    >
      <T keyName="global_form_save" defaultValue="Save" />
    </LoadingButton>
  </DialogActions>
);
