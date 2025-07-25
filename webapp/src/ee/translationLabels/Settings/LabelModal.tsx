import React, { FC } from 'react';
import { Dialog, DialogContent, DialogTitle } from '@mui/material';
import { useTranslate } from '@tolgee/react';
import {
  LabelForm,
  LabelFormValues,
} from 'tg.ee.module/translationLabels/Settings/LabelForm';
import { components } from 'tg.service/apiSchema.generated';

type LabelModel = components['schemas']['LabelModel'];

export const LabelModal: FC<{
  label?: LabelModel;
  open: boolean;
  close: () => void;
  submit: (values: LabelFormValues) => void;
}> = ({ open, close, label, submit }) => {
  const { t } = useTranslate();
  return (
    <Dialog open={open} onClose={close}>
      <DialogTitle>
        {label
          ? t('project_settings_label_edit')
          : t('project_settings_label_add')}
      </DialogTitle>
      <DialogContent
        sx={{ width: 500, maxWidth: '100%' }}
        data-cy="label-modal"
      >
        <LabelForm
          submitText={label ? undefined : t('global_add_button')}
          label={label}
          submit={submit}
          cancel={close}
        />
      </DialogContent>
    </Dialog>
  );
};
