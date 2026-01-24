import React from 'react';
import {
  Dialog,
  DialogContent,
  DialogTitle,
  DialogActions,
  Box,
  Button,
} from '@mui/material';
import { useTranslate, T } from '@tolgee/react';
import { StandardForm } from 'tg.component/common/form/StandardForm';
import { TextField } from 'tg.component/common/form/fields/TextField';
import { Validation } from 'tg.constants/GlobalValidationSchema';
import { BranchNameLabel } from 'tg.ee.module/branching/components/form/BranchNameLabel';
import LoadingButton from 'tg.component/common/form/LoadingButton';

type Props = {
  open: boolean;
  initialName: string;
  onSubmit: (name: string) => void;
  onClose: () => void;
};

export const BranchRenameModal: React.FC<Props> = ({
  open,
  initialName,
  onSubmit,
  onClose,
}) => {
  const { t } = useTranslate();

  return (
    <Dialog open={open} onClose={onClose}>
      <DialogTitle>
        <T keyName="project_branch_rename_title" />
      </DialogTitle>
      <DialogContent sx={{ width: 400, maxWidth: '100%' }}>
        <StandardForm
          initialValues={{ name: initialName }}
          validationSchema={Validation.BRANCH(t)}
          onSubmit={(values) => onSubmit(values.name)}
          formId="branch-rename-form"
          submitButtons={<></>}
        >
          <Box mb={2}>
            <TextField
              name="name"
              label={<BranchNameLabel />}
              required
              size="small"
              autoFocus
              data-cy="branch-name-input"
            />
          </Box>
        </StandardForm>
      </DialogContent>
      <DialogActions>
        <Button data-cy="global-form-cancel-button" onClick={onClose}>
          <T keyName="global_form_cancel" />
        </Button>
        <LoadingButton
          data-cy="global-form-save-button"
          color="primary"
          variant="contained"
          type="submit"
          form="branch-rename-form"
        >
          <T keyName="global_form_save" />
        </LoadingButton>
      </DialogActions>
    </Dialog>
  );
};
