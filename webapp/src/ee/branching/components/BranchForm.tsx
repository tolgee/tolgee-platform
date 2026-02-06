import { StandardForm } from 'tg.component/common/form/StandardForm';
import { components } from 'tg.service/apiSchema.generated';
import React, { FC } from 'react';
import { Box } from '@mui/material';
import { TextField } from 'tg.component/common/form/fields/TextField';
import { FieldLabel } from 'tg.component/FormField';
import { T, useTranslate } from '@tolgee/react';
import { Validation } from 'tg.constants/GlobalValidationSchema';
import { BranchSelectField } from 'tg.component/branching/BranchSelectField';
import { BranchNameLabel } from 'tg.ee.module/branching/components/form/BranchNameLabel';

type BranchModel = components['schemas']['BranchModel'];

export type BranchFormValues = {
  name: string;
  originBranchId: number;
};

export type BranchFormProps = {
  branch?: BranchModel;
  defaultBranch?: BranchModel;
  submit: (values: BranchFormValues) => void;
  cancel?: () => void;
  submitText?: string;
};

export const BranchForm: FC<BranchFormProps> = ({
  submit,
  cancel,
  submitText,
}) => {
  const { t } = useTranslate();
  const initValues = {
    name: '',
    originBranchId: 0,
  } satisfies BranchFormValues;

  const onSubmit = (values: BranchFormValues) => {
    submit(values);
  };

  return (
    <StandardForm
      validationSchema={Validation.BRANCH(t)}
      initialValues={initValues}
      onSubmit={onSubmit}
      formId="branch-form"
      submitButtons={<></>}
    >
      <Box mb={4}>
        <Box display="flex" mb={2}>
          <Box display="grid" flexGrow={1}>
            <FieldLabel>
              <Box display="flex" alignItems="center" gap={1}>
                <BranchNameLabel />
              </Box>
            </FieldLabel>
            <TextField
              size="small"
              name="name"
              required={true}
              data-cy="branch-name-input"
            />
          </Box>
        </Box>
        <Box display="flex" mb={2}>
          <Box display="grid" flexGrow={0}>
            <FieldLabel>
              <T keyName="project_branch_origin_branch" />
            </FieldLabel>
            <BranchSelectField name="originBranchId" />
          </Box>
        </Box>
      </Box>
    </StandardForm>
  );
};
