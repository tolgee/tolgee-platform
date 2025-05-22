import React, { FC } from 'react';
import { Dialog, DialogContent, DialogTitle } from '@mui/material';
import { useTranslate } from '@tolgee/react';
import {
  LabelForm,
  LabelFormValues,
} from 'tg.views/projects/project/components/LabelForm';
import { components } from 'tg.service/apiSchema.generated';

type LabelModel = components['schemas']['LabelModel'];

export const LabelModal: FC<{
  label?: LabelModel;
  open: boolean;
  close: () => void;
  submit: (values: LabelFormValues) => void;
}> = (props) => {
  const { close } = props;
  const { t } = useTranslate();
  return (
    <Dialog open={true} onClose={close}>
      <DialogTitle>{t('project_settings_label_edit')}</DialogTitle>
      <DialogContent sx={{ width: 500, maxWidth: '100%' }}>
        <LabelForm label={props.label} submit={props.submit} cancel={close} />
      </DialogContent>
    </Dialog>
  );
};
