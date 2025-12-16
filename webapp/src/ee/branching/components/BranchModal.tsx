import React, { FC } from 'react';
import { Dialog, DialogContent, DialogTitle } from '@mui/material';
import { useTranslate } from '@tolgee/react';
import { components } from 'tg.service/apiSchema.generated';
import {
  BranchForm,
  BranchFormValues,
} from 'tg.ee.module/branching/components/BranchForm';

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
        <BranchForm
          submitText={branch ? undefined : t('global_create_button')}
          branch={branch}
          submit={submit}
          cancel={close}
        />
      </DialogContent>
    </Dialog>
  );
};
