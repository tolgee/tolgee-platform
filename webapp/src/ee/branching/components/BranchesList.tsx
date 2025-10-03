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

const TableGrid = styled('div')`
  display: grid;
  grid-template-columns: 1fr auto auto;
`;

type BranchModel = components['schemas']['BranchModel'];

export const BranchesList = () => {
  const project = useProject();
  const { t } = useTranslate();
  const [addBranchOpen, setAddBranchOpen] = useState(false);
  const [page, setPage] = useState(0);

  function handleClose() {
    setAddBranchOpen(false);
  }

  const createMutation = useApiMutation({
    url: '/v2/projects/{projectId}/branches',
    method: 'post',
    invalidatePrefix: '/v2/projects/{projectId}/branches',
  });

  const submit = async (values: BranchFormValues) => {
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
          <Typography variant="h5">
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
          renderItem={(l: BranchModel) => <BranchItem branch={l} />}
        />
      </Box>
      {addBranchOpen && branchesLoadable.data && (
        <BranchModal open={addBranchOpen} close={handleClose} submit={submit} />
      )}
    </Box>
  );
};
