import { T } from '@tolgee/react';
import { container } from 'tsyringe';
import { confirmation } from 'tg.hooks/confirmation';
import { TranslationActions } from 'tg.store/project/TranslationActions';

const actions = container.resolve(TranslationActions);

export const useLeaveEditConfirmationPagination = () => {
  const confirmationData = actions.useSelector((s) => {
    if (s.editing?.data?.initialValue !== s.editing?.data?.newValue) {
      return s.editing!.data;
    }
    return null;
  });

  return (onConfirm: () => void, onCancel?: () => void) => {
    const cleanOnConfirm = () => {
      onConfirm();
      actions.otherEditionConfirm.dispatch();
    };

    if (confirmationData === null) {
      cleanOnConfirm();
    }
    useLeaveEditConfirmation(confirmationData)(cleanOnConfirm, onCancel);
  };
};

export const useLeaveEditConfirmationOtherEdit = () => {
  const confirmationData = actions.useSelector((s) => {
    if (s.editingAfterConfirmation) {
      return s.editing!.data;
    }
    return null;
  });

  return useLeaveEditConfirmation(confirmationData);
};

const useLeaveEditConfirmation = (confirmationData) => {
  return (onConfirm: () => void, onCancel?: () => void) => {
    if (confirmationData) {
      confirmation({
        title: <T>translations_leave_save_confirmation</T>,
        message: (
          <T parameters={confirmationData}>
            translations_leave_save_confirmation_message
          </T>
        ),
        cancelButtonText: <T>back_to_editing</T>,
        confirmButtonText: <T>discard_changes</T>,
        onConfirm,
        onCancel,
      });
    }
  };
};
