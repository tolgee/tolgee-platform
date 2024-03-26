import { useState } from 'react';
import { ConfirmationDialogProps } from 'tg.component/common/ConfirmationDialog';

export const useConfirmationDialogService = () => {
  const [confirmationDialog, setConfirmationDialog] =
    useState<ConfirmationDialogProps>();

  const actions = {
    openConfirmationDialog(options: ConfirmationDialogProps) {
      setConfirmationDialog({ ...options, open: true });
    },
    closeConfirmation() {
      setConfirmationDialog((state) => ({ ...state, open: false }));
    },
  };

  const state = confirmationDialog;

  return { state, actions };
};
