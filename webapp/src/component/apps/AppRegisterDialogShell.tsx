import React from 'react';
import { Dialog, DialogTitle } from '@mui/material';

type Props = {
  open: boolean;
  onClose: () => void;
  title: React.ReactNode;
  dataCy: string;
  children: React.ReactNode;
};

/** Dialog frame for the app registration steps. */
export const AppRegisterDialogShell = ({
  open,
  onClose,
  title,
  dataCy,
  children,
}: Props) => (
  <Dialog
    open={open}
    onClose={onClose}
    maxWidth="sm"
    fullWidth
    data-cy={dataCy}
  >
    <DialogTitle>{title}</DialogTitle>
    {children}
  </Dialog>
);
