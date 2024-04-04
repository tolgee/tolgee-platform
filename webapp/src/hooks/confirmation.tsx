import { T } from '@tolgee/react';
import { ConfirmationDialogProps } from '../component/common/ConfirmationDialog';
import { globalContext } from 'tg.globalContext/globalActions';

export const confirmation = (options: ConfirmationDialogProps = {}) => {
  globalContext.actions?.openConfirmationDialog({ ...options });
};

export const confirmDiscardUnsaved = (
  options: ConfirmationDialogProps = {}
) => {
  globalContext.actions?.openConfirmationDialog({
    title: <T keyName="confirmation_discard_unsaved_title" />,
    message: <T keyName="confirmation_discard_unsaved_message" />,
    confirmButtonText: <T keyName="confirmation_discard_unsaved_confirm" />,
    ...options,
  });
};
