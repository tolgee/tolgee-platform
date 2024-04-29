import { ReactNode, useEffect, useState } from 'react';
import { PropTypes, TextField } from '@mui/material';
import Box from '@mui/material/Box';
import Button from '@mui/material/Button';
import Dialog from '@mui/material/Dialog';
import DialogActions from '@mui/material/DialogActions';
import DialogContent from '@mui/material/DialogContent';
import DialogContentText from '@mui/material/DialogContentText';
import DialogTitle from '@mui/material/DialogTitle';
import { T } from '@tolgee/react';

export class ConfirmationDialogProps {
  open?: boolean = true;
  message?: ReactNode = (<T keyName="confirmation_dialog_message" />);
  confirmButtonText?: ReactNode = (<T keyName="confirmation_dialog_confirm" />);
  cancelButtonText?: ReactNode = (<T keyName="confirmation_dialog_cancel" />);
  title?: ReactNode = (<T keyName="confirmation_dialog_title" />);
  hardModeText?: string | null = null;
  confirmButtonColor?: PropTypes.Color = 'primary';
  cancelButtonColor?: PropTypes.Color = 'default';

  onCancel?: () => void = () => {};

  onConfirm?: () => void = () => {};
}

export default function ConfirmationDialog(props: ConfirmationDialogProps) {
  props = { ...new ConfirmationDialogProps(), ...props };

  const [input, setInput] = useState('');

  useEffect(() => {
    setInput('');
  }, [props.hardModeText]);

  const disabled = props.hardModeText && props.hardModeText !== input;

  return (
    <Dialog
      open={!!props.open}
      onClose={props.onCancel}
      aria-labelledby="alert-dialog-title"
      aria-describedby="alert-dialog-description"
      data-cy="global-confirmation-dialog"
    >
      <DialogTitle id="alert-dialog-title">{props.title}</DialogTitle>
      <form
        onSubmit={(e) => {
          if (!disabled && props.onConfirm) {
            setInput('');
            props.onConfirm();
          }
          e.preventDefault();
        }}
      >
        <DialogContent>
          <DialogContentText id="alert-dialog-description">
            {props.message}
          </DialogContentText>
          {props.hardModeText && (
            <Box mt={2} mb={1}>
              <TextField
                variant="standard"
                data-cy={'global-confirmation-hard-mode-text-field'}
                fullWidth={true}
                label={
                  <T
                    keyName="hard_mode_confirmation_rewrite_text"
                    params={{ text: props.hardModeText }}
                  />
                }
                value={input}
                onChange={(e) => setInput(e.target.value)}
              />
            </Box>
          )}
        </DialogContent>
        <DialogActions>
          <Button
            data-cy="global-confirmation-cancel"
            onClick={() => {
              setInput('');
              props.onCancel && props.onCancel();
            }}
            type="button"
            sx={{ color: props.cancelButtonColor }}
          >
            {props.cancelButtonText}
          </Button>
          <Button
            data-cy="global-confirmation-confirm"
            color="primary"
            autoFocus
            disabled={!!disabled}
            type="submit"
          >
            {props.confirmButtonText}
          </Button>
        </DialogActions>
      </form>
    </Dialog>
  );
}
