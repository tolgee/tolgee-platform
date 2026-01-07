import React, { useState } from 'react';
import { Box, Button, styled, Typography } from '@mui/material';
import { T, useTranslate } from '@tolgee/react';
import { useProject } from 'tg.hooks/useProject';
import { Plus } from '@untitled-ui/icons-react';
import { BranchItem } from 'tg.ee.module/branching/components/BranchItem';
import { useApiMutation, useApiQuery } from 'tg.service/http/useQueryApi';
import { PaginatedHateoasList } from 'tg.component/common/list/PaginatedHateoasList';
import { components } from 'tg.service/apiSchema.generated';
import { BranchModal } from 'tg.ee.module/branching/components/BranchModal';
import { BranchFormValues } from 'tg.ee.module/branching/components/BranchForm';
import { confirmation } from 'tg.hooks/confirmation';
import { useHistory } from 'react-router-dom';
import { LINKS } from 'tg.constants/links';
import { BranchRenameModal } from './BranchRenameModal';
import { BranchNameChipNode } from 'tg.component/branching/BranchNameChip';

const TableGrid = styled('div')`
  display: grid;
  grid-template-columns: 1fr auto auto;
`;

type BranchModel = components['schemas']['BranchModel'];
type DryRunMergeBranchRequest =
  components['schemas']['DryRunMergeBranchRequest'];

export const BranchesList = () => {
  const project = useProject();
  const { t } = useTranslate();
  const history = useHistory();

  const [addBranchOpen, setAddBranchOpen] = useState(false);
  const [page, setPage] = useState(0);
  const [renameBranch, setRenameBranch] = useState<BranchModel | null>(null);

  function handleCloseMergeIntoModal() {
    setAddBranchOpen(false);
  }

  const createMutation = useApiMutation({
    url: '/v2/projects/{projectId}/branches',
    method: 'post',
    invalidatePrefix: '/v2/projects/{projectId}/branches',
  });

  const createMergeMutation = useApiMutation({
    url: '/v2/projects/{projectId}/branches/merge/preview',
    method: 'post',
  });

  const deleteMutation = useApiMutation({
    url: '/v2/projects/{projectId}/branches/{branchId}',
    method: 'delete',
    invalidatePrefix: '/v2/projects/{projectId}/branches',
  });

  const renameMutation = useApiMutation({
    url: '/v2/projects/{projectId}/branches/{branchId}',
    method: 'post',
    invalidatePrefix: '/v2/projects/{projectId}/branches',
  });

  const createBranchSubmit = async (values: BranchFormValues) => {
    await createMutation.mutateAsync({
      path: { projectId: project.id },
      content: {
        'application/json': {
          ...values,
        },
      },
    });
    setAddBranchOpen(false);
  };

  const mergeIntoSubmit = async (values: DryRunMergeBranchRequest) => {
    const response = await createMergeMutation.mutateAsync({
      path: { projectId: project.id },
      content: {
        'application/json': {
          sourceBranchId: values.sourceBranchId,
        },
      },
    });
    history.push(
      LINKS.PROJECT_BRANCHES_MERGE.build({
        projectId: project.id,
        mergeId: response.id,
      })
    );
  };

  const deleteBranch = async (branch: BranchModel) => {
    confirmation({
      message: (
        <T
          keyName="project_branch_delete_confirmation"
          params={{ branchName: branch.name, b: <b /> }}
        />
      ),
      confirmButtonText: <T keyName="confirmation_dialog_delete" />,
      async onConfirm() {
        await deleteMutation.mutateAsync({
          path: { projectId: project.id, branchId: branch.id },
        });
      },
    });
  };

  const handleMergeInto = (branch: BranchModel) => {
    if (branch.merge && !branch.merge.mergedAt) {
      handleMergeDetail(branch);
    } else {
      confirmation({
        message: (
          <T
            keyName="branch_merges_create_title"
            params={{
              name: branch?.name,
              branch: <BranchNameChipNode />,
              targetName: branch?.originBranchName,
            }}
          />
        ),
        confirmButtonText: <T keyName="branch_merges_create_button" />,
        async onConfirm() {
          await mergeIntoSubmit({
            sourceBranchId: branch.id,
          });
        },
      });
    }
  };

  const handleRenameSubmit = async (name: string) => {
    if (!renameBranch) return;
    await renameMutation.mutateAsync({
      path: { projectId: project.id, branchId: renameBranch.id },
      content: { 'application/json': { name } },
    });
    setRenameBranch(null);
  };

  const handleMergeDetail = (branch: BranchModel) => {
    history.push(
      LINKS.PROJECT_BRANCHES_MERGE.build({
        projectId: project.id,
        mergeId: branch.merge!.id,
      })
    );
  };

  const canEditBranches = true; // TODO satisfiesPermission('branches.edit')

  const branchesLoadable = useApiQuery({
    url: '/v2/projects/{projectId}/branches',
    method: 'get',
    path: { projectId: project.id },
    query: {
      size: 25,
      page: page,
    },
  });

  return (
    <Box mb={6}>
      <Box>
        <Box
          mt={4}
          mb={3}
          display="flex"
          justifyContent="space-between"
          alignItems="center"
        >
          <Typography variant="h4">
            <T keyName="branches_title" />
          </Typography>
          {canEditBranches && (
            <Button
              color="primary"
              variant="contained"
              startIcon={<Plus width={19} height={19} />}
              onClick={() => setAddBranchOpen(true)}
              data-cy="project-settings-branches-add"
            >
              {t('project_branches_add_button')}
            </Button>
          )}
        </Box>

        <PaginatedHateoasList
          loadable={branchesLoadable}
          onPageChange={setPage}
          listComponent={TableGrid}
          data-cy="project-settings-labels-list"
          emptyPlaceholder={
            <Box m={2} display="flex" justifyContent="center">
              <Typography color="textSecondary">
                {t('project_settings_no_labels_yet')}
              </Typography>
            </Box>
          }
          renderItem={(l: BranchModel) => (
            <BranchItem
              branch={l}
              onRemove={deleteBranch}
              onRename={(branch) => setRenameBranch(branch)}
              onMergeInto={() => handleMergeInto(l)}
              onMergeDetail={() => handleMergeDetail(l)}
            />
          )}
        />
      </Box>
      {branchesLoadable.data && (
        <>
          <BranchModal
            open={addBranchOpen}
            close={handleCloseMergeIntoModal}
            submit={createBranchSubmit}
          />
          {renameBranch && (
            <BranchRenameModal
              open={Boolean(renameBranch)}
              initialName={renameBranch.name}
              onSubmit={handleRenameSubmit}
              onClose={() => setRenameBranch(null)}
            />
          )}
        </>
      )}
    </Box>
  );
};
