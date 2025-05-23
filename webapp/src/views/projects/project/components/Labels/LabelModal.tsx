import React, { FC } from 'react';
import { Dialog, DialogContent, DialogTitle } from '@mui/material';
import { useTranslate } from '@tolgee/react';
import {
  LabelForm,
  LabelFormValues,
} from 'tg.views/projects/project/components/Labels/LabelForm';
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
      <DialogTitle>
        {t(
          props.label
            ? 'project_settings_label_edit'
            : 'project_settings_label_add'
        )}
      </DialogTitle>
      <DialogContent sx={{ width: 500, maxWidth: '100%' }}>
        <LabelForm
          submitText={props.label ? undefined : t('global_add_button')}
          label={props.label}
          submit={props.submit}
          cancel={close}
        />
      </DialogContent>
    </Dialog>
  );
};
