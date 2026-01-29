import React, { FC } from 'react';
import {
  Dialog,
  DialogContent,
  DialogTitle,
  DialogActions,
  Button,
} from '@mui/material';
import { T, useTranslate } from '@tolgee/react';
import { components } from 'tg.service/apiSchema.generated';
import {
  BranchForm,
  BranchFormValues,
} from 'tg.ee.module/branching/components/BranchForm';
import LoadingButton from 'tg.component/common/form/LoadingButton';

type BranchModel = components['schemas']['BranchModel'];

export const BranchModal: FC<{
  branch?: BranchModel;
  open: boolean;
  close: () => void;
  submit: (values: BranchFormValues) => void;
}> = ({ open, close, branch, submit }) => {
  const { t } = useTranslate();
  return (
    <Dialog open={open} onClose={close}>
      <DialogTitle>
        {branch ? t('project_branch_edit') : t('project_branch_add')}
      </DialogTitle>
      <DialogContent
        sx={{ width: 500, maxWidth: '100%' }}
        data-cy="label-modal"
      >
        <BranchForm branch={branch} submit={submit} />
      </DialogContent>
      <DialogActions>
        <Button data-cy="global-form-cancel-button" onClick={close}>
          <T keyName="global_form_cancel" />
        </Button>
        <LoadingButton
          data-cy="global-form-save-button"
          color="primary"
          variant="contained"
          type="submit"
          form="branch-form"
        >
          {branch ? (
            <T keyName="global_form_save" />
          ) : (
            t('global_create_button')
          )}
        </LoadingButton>
      </DialogActions>
    </Dialog>
  );
};
