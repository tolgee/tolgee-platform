import React, { useMemo } from 'react';
import { Box, Dialog, DialogContent, DialogTitle, styled } from '@mui/material';
import { T, useTranslate } from '@tolgee/react';

import { StandardForm } from 'tg.component/common/form/StandardForm';
import { FieldLabel } from 'tg.component/FormField';
import { useProject } from 'tg.hooks/useProject';
import { useApiQuery } from 'tg.service/http/useQueryApi';
import { components } from 'tg.service/apiSchema.generated';
import { LoadableType } from 'tg.component/common/form/StandardForm';
import { BranchSelectField } from 'tg.component/branching/BranchSelectField';
import { Validation } from 'tg.constants/GlobalValidationSchema';
import { BranchNameChipNode } from 'tg.component/branching/BranchNameChip';

type BranchModel = components['schemas']['BranchModel'];
type DryRunMergeBranchRequest =
  components['schemas']['DryRunMergeBranchRequest'];

type Props = {
  open: boolean;
  close: () => void;
  submit: (values: DryRunMergeBranchRequest) => void;
  saveActionLoadable?: LoadableType;
  sourceBranch?: BranchModel | null;
};

const defaultValues: DryRunMergeBranchRequest = {
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

export const BranchMergeCreateModal: React.FC<Props> = ({
  open,
  close,
  submit,
  saveActionLoadable,
  sourceBranch,
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
    if (sourceBranch && values.targetBranchId) {
      submit({
        sourceBranchId: sourceBranch.id,
        targetBranchId: values.targetBranchId,
      });
    }
  };

  const initialValues = useMemo(
    () => ({
      ...defaultValues,
      sourceBranchId: sourceBranch?.id ?? 0,
    }),
    [sourceBranch]
  );

  const isDisabled =
    branches.length < 2 ||
    branchesLoadable.isLoading ||
    Boolean(branchesLoadable.error);

  return (
    <Dialog open={open} onClose={close} maxWidth="sm" fullWidth>
      <DialogTitle>
        <T
          keyName="branch_merges_create_title"
          params={{ name: sourceBranch?.name, branch: <BranchNameChipNode /> }}
        />
      </DialogTitle>
      <DialogContent data-cy="branch-merge-create-modal">
        <StandardForm<DryRunMergeBranchRequest>
          initialValues={initialValues}
          onSubmit={handleSubmit}
          onCancel={close}
          validationSchema={Validation.BRANCH_MERGE(t)}
          submitButtonInner={t('branch_merges_create_button')}
          saveActionLoadable={saveActionLoadable}
          disabled={isDisabled}
        >
          {(_) => (
            <FormLayout>
              <SelectorColumn>
                <FieldLabel>
                  <T keyName="branch_merges_target_branch" />
                </FieldLabel>
                <Box data-cy="branch-merge-target-select">
                  <BranchSelectField
                    name="targetBranchId"
                    hiddenIds={sourceBranch ? [sourceBranch?.id] : undefined}
                  />
                </Box>
              </SelectorColumn>
            </FormLayout>
          )}
        </StandardForm>
      </DialogContent>
    </Dialog>
  );
};
