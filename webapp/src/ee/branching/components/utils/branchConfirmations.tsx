import { T } from '@tolgee/react';
import { ConfirmationDialogProps } from 'tg.component/common/ConfirmationDialog';
import { confirmation } from 'tg.hooks/confirmation';

type ConfirmProtectedOptions = {
  branchName: string;
  willProtect: boolean;
} & Omit<ConfirmationDialogProps, 'title' | 'message' | 'confirmButtonText'>;

export const confirmProtected = ({
  branchName,
  willProtect,
  ...options
}: ConfirmProtectedOptions) => {
  confirmation({
    title: willProtect ? (
      <T keyName="project_branch_protect_confirmation_title" />
    ) : (
      <T keyName="project_branch_unprotect_confirmation_title" />
    ),
    message: willProtect ? (
      <T
        keyName="project_branch_protect_confirmation"
        params={{ branchName, b: <b /> }}
      />
    ) : (
      <T
        keyName="project_branch_unprotect_confirmation"
        params={{ branchName, b: <b /> }}
      />
    ),
    confirmButtonText: willProtect ? (
      <T keyName="confirmation_dialog_button_protect" />
    ) : (
      <T keyName="confirmation_dialog_button_unprotect" />
    ),
    ...options,
  });
};
