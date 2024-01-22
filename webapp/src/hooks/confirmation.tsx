import { T } from '@tolgee/react';
import { ConfirmationDialogProps } from '../component/common/ConfirmationDialog';
import { globalActions } from '../store/global/GlobalActions';

export const confirmation = (options: ConfirmationDialogProps = {}) => {
  globalActions.openConfirmation.dispatch({ ...options });
};

export const confirmDiscardUnsaved = (
  options: ConfirmationDialogProps = {}
) => {
  globalActions.openConfirmation.dispatch({
    title: <T keyName="confirmation_discard_unsaved_title" />,
    message: <T keyName="confirmation_discard_unsaved_message" />,
    confirmButtonText: <T keyName="confirmation_discard_unsaved_confirm" />,
    ...options,
  });
};
