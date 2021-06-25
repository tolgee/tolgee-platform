import { ReactNode, useEffect, useState } from 'react';
import { PropTypes, TextField } from '@material-ui/core';
import Box from '@material-ui/core/Box';
import Button from '@material-ui/core/Button';
import Dialog from '@material-ui/core/Dialog';
import DialogActions from '@material-ui/core/DialogActions';
import DialogContent from '@material-ui/core/DialogContent';
import DialogContentText from '@material-ui/core/DialogContentText';
import DialogTitle from '@material-ui/core/DialogTitle';
import { T } from '@tolgee/react';

export class ConfirmationDialogProps {
  open?: boolean = true;
  message?: ReactNode = (<T>confirmation_dialog_message</T>);
  confirmButtonText?: ReactNode = (<T>confirmation_dialog_confirm</T>);
  cancelButtonText?: ReactNode = (<T>confirmation_dialog_cancel</T>);
  title?: ReactNode = (<T>confirmation_dialog_title</T>);
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
            <Box>
              <TextField
                data-cy={'global-confirmation-hard-mode-text-field'}
                fullWidth={true}
                label={
                  <T parameters={{ text: props.hardModeText }}>
                    hard_mode_confirmation_rewrite_text
                  </T>
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
            color={props.cancelButtonColor}
          >
            {props.cancelButtonText}
          </Button>
          <Button
            data-cy="global-confirmation-confirm"
            color={props.confirmButtonColor}
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
