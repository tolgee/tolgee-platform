import React from 'react';
import {
  Box,
  Dialog,
  DialogContent,
  DialogTitle,
  Typography,
  styled,
} from '@mui/material';
import { ArrowRight } from '@untitled-ui/icons-react';
import { T, useTranslate } from '@tolgee/react';

import { StandardForm } from 'tg.component/common/form/StandardForm';
import { FieldLabel } from 'tg.component/FormField';
import { useProject } from 'tg.hooks/useProject';
import { useApiQuery } from 'tg.service/http/useQueryApi';
import { components } from 'tg.service/apiSchema.generated';
import { LoadableType } from 'tg.component/common/form/StandardForm';
import { BranchSelectField } from 'tg.component/branching/BranchSelectField';
import { TextField } from 'tg.component/common/form/fields/TextField';
import { Validation } from 'tg.constants/GlobalValidationSchema';

type DryRunMergeBranchRequest =
  components['schemas']['DryRunMergeBranchRequest'];

type Props = {
  open: boolean;
  close: () => void;
  submit: (values: DryRunMergeBranchRequest) => void;
  saveActionLoadable?: LoadableType;
};

const defaultValues: DryRunMergeBranchRequest = {
  name: '',
  sourceBranchId: 0,
  targetBranchId: 0,
};

const FormLayout = styled(Box)`
  display: flex;
  align-items: center;
  gap: ${({ theme }) => theme.spacing(3)};
  flex-wrap: wrap;
  margin-top: ${({ theme }) => theme.spacing(1)};
  margin-bottom: ${({ theme }) => theme.spacing(4)};
`;

const SelectorColumn = styled(Box)`
  display: grid;
  gap: ${({ theme }) => theme.spacing(1)};
  min-width: 240px;
  flex: 1;
`;

const Placeholder = styled(Typography)`
  color: ${({ theme }) => theme.palette.text.secondary};
`;

export const BranchMergeCreateModal: React.FC<Props> = ({
  open,
  close,
  submit,
  saveActionLoadable,
}) => {
  const { t } = useTranslate();
  const project = useProject();

  const branchesLoadable = useApiQuery({
    url: '/v2/projects/{projectId}/branches',
    method: 'get',
    path: { projectId: project.id },
    query: {
      size: 50,
      page: 0,
    },
  });

  const branches = branchesLoadable.data?._embedded?.branches ?? [];

  const handleSubmit = (values: DryRunMergeBranchRequest) => {
    if (values.sourceBranchId != null && values.targetBranchId != null) {
      submit({
        name: values.name,
        sourceBranchId: values.sourceBranchId,
        targetBranchId: values.targetBranchId,
      });
    }
  };

  const isDisabled =
    branches.length < 2 ||
    branchesLoadable.isLoading ||
    Boolean(branchesLoadable.error);

  return (
    <Dialog open={open} onClose={close} maxWidth="sm" fullWidth>
      <DialogTitle>{t('branch_merges_create_title')}</DialogTitle>
      <DialogContent data-cy="branch-merge-create-modal">
        <StandardForm<DryRunMergeBranchRequest>
          initialValues={defaultValues}
          onSubmit={handleSubmit}
          onCancel={close}
          validationSchema={Validation.BRANCH_MERGE(t)}
          submitButtonInner={t('branch_merges_create_button')}
          saveActionLoadable={saveActionLoadable}
          disabled={isDisabled}
        >
          {(_) => (
            <FormLayout>
              <Box>
                <Box>
                  <FieldLabel>
                    <T keyName="project_branch_merge_name" />
                  </FieldLabel>
                  <TextField size="small" name="name" required={true} />
                </Box>
                {branches.length < 2 && !branchesLoadable.isLoading && (
                  <Placeholder variant="body2">
                    <T keyName="branch_merges_not_enough_branches" />
                  </Placeholder>
                )}

                <Box display="flex" columnGap={3}>
                  <SelectorColumn>
                    <FieldLabel>
                      <T keyName="branch_merges_source_branch" />
                    </FieldLabel>
                    <Box data-cy="branch-merge-source-select">
                      <BranchSelectField
                        name="sourceBranchId"
                        hideDefault={true}
                      />
                    </Box>
                  </SelectorColumn>

                  <Box mt={5}>
                    <ArrowRight width={22} height={22} />
                  </Box>

                  <SelectorColumn>
                    <FieldLabel>
                      <T keyName="branch_merges_target_branch" />
                    </FieldLabel>
                    <Box data-cy="branch-merge-target-select">
                      <BranchSelectField name="targetBranchId" />
                    </Box>
                  </SelectorColumn>
                </Box>
              </Box>
            </FormLayout>
          )}
        </StandardForm>
      </DialogContent>
    </Dialog>
  );
};
