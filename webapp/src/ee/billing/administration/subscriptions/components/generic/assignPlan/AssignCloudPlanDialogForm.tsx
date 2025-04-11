import React, { FC, ReactNode } from 'react';
import { Form } from 'formik';
import {
  Button,
  DialogActions,
  DialogContent,
  DialogTitle,
} from '@mui/material';
import { T } from '@tolgee/react';
import { AssignPlanDialogSaveButton } from './AssignPlanDialogSaveButton';

type AssignCloudPlanDialogFormProps = {
  fields: ReactNode;
  onClose: () => void;
  saveMutation: { isLoading: boolean };
};

export const AssignCloudPlanDialogForm: FC<AssignCloudPlanDialogFormProps> = ({
  fields,
  onClose,
  saveMutation,
}) => {
  return (
    <Form>
      <DialogTitle>
        <T keyName="administration-subscription-assign-plan-dialog-title" />
      </DialogTitle>
      <DialogContent sx={{ display: 'grid', gap: '16px' }}>
        {fields}
      </DialogContent>
      <DialogActions>
        <Button onClick={onClose}>
          <T keyName="global_cancel_button" />
        </Button>
        <AssignPlanDialogSaveButton saveMutation={saveMutation} />
      </DialogActions>
    </Form>
  );
};
