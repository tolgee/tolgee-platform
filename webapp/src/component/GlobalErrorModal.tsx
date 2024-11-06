import { Dialog, DialogContent, IconButton, styled } from '@mui/material';
import { XClose } from '@untitled-ui/icons-react';

import GlobalErrorPage from './common/GlobalErrorPage';
import {
  useGlobalActions,
  useGlobalContext,
} from 'tg.globalContext/GlobalContext';

const CloseIcon = styled('div')`
  position: absolute;
  top: ${({ theme }) => theme.spacing(1)};
  right: ${({ theme }) => theme.spacing(1)};
  display: flex;
  justify-content: flex-end;
`;

export const GlobalErrorModal = () => {
  const error = useGlobalContext((c) => c.globalError);
  const { setGlobalError } = useGlobalActions();

  const handleClose = () => {
    setGlobalError(undefined);
  };

  return error ? (
    <Dialog open={true} maxWidth="lg" onClose={handleClose}>
      <CloseIcon>
        <IconButton onClick={handleClose} size="large">
          <XClose />
        </IconButton>
      </CloseIcon>

      <DialogContent>
        <GlobalErrorPage error={error} />
      </DialogContent>
    </Dialog>
  ) : null;
};
