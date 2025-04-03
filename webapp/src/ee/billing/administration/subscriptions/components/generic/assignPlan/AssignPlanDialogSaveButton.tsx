import React, { FC } from 'react';
import LoadingButton from 'tg.component/common/form/LoadingButton';
import { T } from '@tolgee/react';

type AssignPlanDialogSaveButtonProps = {
  saveMutation: { isLoading: boolean };
};

export const AssignPlanDialogSaveButton: FC<
  AssignPlanDialogSaveButtonProps
> = ({ saveMutation }) => {
  return (
    <LoadingButton
      type="submit"
      loading={saveMutation.isLoading}
      color="primary"
      variant="contained"
      data-cy="administration-subscriptions-assign-plan-save-button"
    >
      <T keyName="administartion_billing_assign-plan-save_button" />
    </LoadingButton>
  );
};
