import { Dialog, DialogContent, IconButton, styled } from '@mui/material';
import { Close } from '@mui/icons-material';
import { useSelector } from 'react-redux';
import { AppState } from 'tg.store/index';
import { container } from 'tsyringe';
import { ErrorActions } from 'tg.store/global/ErrorActions';

import GlobalError from './common/GlobalError';

const errorActions = container.resolve(ErrorActions);

const CloseIcon = styled('div')`
  position: absolute;
  top: ${({ theme }) => theme.spacing(1)};
  right: ${({ theme }) => theme.spacing(1)};
  display: flex;
  justify-content: flex-end;
`;

export const GlobalErrorModal = () => {
  const error = useSelector((state: AppState) => state.error.error);

  const handleClose = () => {
    errorActions.globalError.dispatch(null);
  };

  return error ? (
    <Dialog open={true} maxWidth="lg" onClose={handleClose}>
      <CloseIcon>
        <IconButton onClick={handleClose} size="large">
          <Close />
        </IconButton>
      </CloseIcon>

      <DialogContent>
        <GlobalError error={error} />
      </DialogContent>
    </Dialog>
  ) : null;
};
