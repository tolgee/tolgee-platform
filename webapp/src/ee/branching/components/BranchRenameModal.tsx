import React from 'react';
import { Dialog, DialogContent, DialogTitle, Box } from '@mui/material';
import { useTranslate, T } from '@tolgee/react';
import { StandardForm } from 'tg.component/common/form/StandardForm';
import { TextField } from 'tg.component/common/form/fields/TextField';
import { Validation } from 'tg.constants/GlobalValidationSchema';
import { BranchNameLabel } from 'tg.ee.module/branching/components/form/BranchNameLabel';

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
          onCancel={onClose}
          submitButtonInner={<T keyName="global_rename_button" />}
        >
          <Box mb={2}>
            <TextField
              name="name"
              label={<BranchNameLabel />}
              required
              size="small"
              autoFocus
            />
          </Box>
        </StandardForm>
      </DialogContent>
    </Dialog>
  );
};
