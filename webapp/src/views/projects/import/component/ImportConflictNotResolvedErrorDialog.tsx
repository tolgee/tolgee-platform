import React, { FunctionComponent } from 'react';
import {
  Button,
  Dialog,
  DialogActions,
  DialogContent,
  DialogContentText,
  DialogTitle,
} from '@mui/material';
import { T } from '@tolgee/react';

export const ImportConflictNotResolvedErrorDialog: FunctionComponent<{
  open: boolean;
  onClose: () => void;
  onResolve: () => void;
}> = (props) => {
  return (
    <div>
      <Dialog
        open={props.open}
        onClose={props.onClose}
        aria-labelledby="import-not-resolved-error-dialog-title"
        data-cy="import-conflicts-not-resolved-dialog"
      >
        <DialogTitle id="import-not-resolved-error-dialog-title">
          <T keyName="import_not_resolved_error_dialog_title" />
        </DialogTitle>
        <DialogContent>
          <DialogContentText>
            <T keyName="import_not_resolved_error_dialog_message_text" />
          </DialogContentText>
        </DialogContent>
        <DialogActions>
          <Button
            onClick={props.onClose}
            color="secondary"
            data-cy="import-conflicts-not-resolved-dialog-cancel-button"
          >
            <T keyName="import_not_resolved_error_dialog_cancel_button" />
          </Button>
          <Button
            onClick={props.onResolve}
            color="primary"
            data-cy="import-conflicts-not-resolved-dialog-resolve-button"
          >
            <T keyName="import_resolve_conflicts_button" />
          </Button>
        </DialogActions>
      </Dialog>
    </div>
  );
};
