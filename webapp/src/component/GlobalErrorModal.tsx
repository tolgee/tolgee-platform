import { Dialog, DialogContent, IconButton, Theme } from '@mui/material';
import makeStyles from '@mui/styles/makeStyles';
import { Close } from '@mui/icons-material';
import { useSelector } from 'react-redux';
import { AppState } from 'tg.store/index';
import { container } from 'tsyringe';
import { ErrorActions } from 'tg.store/global/ErrorActions';

import GlobalError from './common/GlobalError';

const errorActions = container.resolve(ErrorActions);

const useStyle = makeStyles<Theme>((theme) => ({
  closeIcon: {
    position: 'absolute',
    top: theme.spacing(1),
    right: theme.spacing(1),
    display: 'flex',
    justifyContent: 'flex-end',
  },
}));

export const GlobalErrorModal = () => {
  const classes = useStyle();
  const error = useSelector((state: AppState) => state.error.error);

  const handleClose = () => {
    errorActions.globalError.dispatch(null);
  };

  return error ? (
    <Dialog open={true} maxWidth="lg" onClose={handleClose}>
      <div className={classes.closeIcon}>
        <IconButton onClick={handleClose} size="large">
          <Close />
        </IconButton>
      </div>

      <DialogContent>
        <GlobalError error={error} />
      </DialogContent>
    </Dialog>
  ) : null;
};
