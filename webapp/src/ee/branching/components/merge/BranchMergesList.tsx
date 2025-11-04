import React, { useState } from 'react';
import { T, useTranslate } from '@tolgee/react';
import { Box, Button, Typography, styled } from '@mui/material';
import { Plus } from '@untitled-ui/icons-react';
import { useProject } from 'tg.hooks/useProject';
import { useApiQuery, useApiMutation } from 'tg.service/http/useQueryApi';
import { components } from 'tg.service/apiSchema.generated';
import { PaginatedHateoasList } from 'tg.component/common/list/PaginatedHateoasList';
import { BranchMergeRow } from 'tg.ee.module/branching/components/merge/BranchMergeRow';
import { BranchMergeCreateModal } from 'tg.ee.module/branching/components/BranchMergeCreateModal';
import { UseQueryResult } from 'react-query';
import { ApiError } from 'tg.service/http/ApiError';
import { confirmation } from 'tg.hooks/confirmation';

type BranchMergeModel = components['schemas']['BranchMergeModel'];
type DryRunMergeBranchRequest =
  components['schemas']['DryRunMergeBranchRequest'];

type BranchMergeListResponse = {
  _embedded?: {
    branchMerges?: BranchMergeModel[];
  };
  page?: components['schemas']['PageMetadata'];
};

const MergeList = styled('div')`
  display: flex;
  flex-direction: column;
  gap: ${({ theme }) => theme.spacing(2)};
`;

const ListHeader = styled(Box)`
  margin-top: ${({ theme }) => theme.spacing(4)};
  margin-bottom: ${({ theme }) => theme.spacing(3)};
  display: flex;
  justify-content: space-between;
  align-items: center;
`;

const EmptyPlaceholder = styled(Box)`
  margin: ${({ theme }) => theme.spacing(2)};
  display: flex;
  justify-content: center;
`;

export const BranchMergesList: React.FC = () => {
  const { t } = useTranslate();
  const project = useProject();
  const [page, setPage] = useState(0);
  const [createOpen, setCreateOpen] = useState(false);
  const [deletingId, setDeletingId] = useState<number | null>(null);

  const mergesLoadable = useApiQuery<any, any, any>({
    url: '/v2/projects/{projectId}/branches/merge' as any,
    method: 'get' as any,
    path: { projectId: project.id },
    query: {
      size: 25,
      page,
    },
  } as any) as UseQueryResult<BranchMergeListResponse, ApiError>;

  const createMergeMutation = useApiMutation<any, any, any>({
    url: '/v2/projects/{projectId}/branches/merge/preview' as any,
    method: 'post' as any,
  } as any);

  const deleteMergeMutation = useApiMutation<any, any, any>({
    url: '/v2/projects/{projectId}/branches/merge/{mergeId}' as any,
    method: 'delete' as any,
  } as any);

  const handleCreateMerge = () => setCreateOpen(true);

  const handleDelete = (merge: BranchMergeModel) => {
    confirmation({
      message: (
        <T
          keyName="branch_merges_delete_confirmation"
          params={{
            source: merge.sourceBranch.name,
            target: merge.targetBranch.name,
            b: <b />,
          }}
        />
      ),
      confirmButtonText: <T keyName="confirmation_dialog_delete" />,
      async onConfirm() {
        try {
          setDeletingId(merge.id);
          await deleteMergeMutation.mutateAsync({
            path: { projectId: project.id, mergeId: merge.id },
          });
          await mergesLoadable.refetch?.();
        } finally {
          setDeletingId(null);
        }
      },
    });
  };

  const handleSubmit = async (values: DryRunMergeBranchRequest) => {
    await createMergeMutation.mutateAsync({
      path: { projectId: project.id },
      content: {
        'application/json': {
          name: values.name,
          sourceBranchId: values.sourceBranchId,
          targetBranchId: values.targetBranchId,
        },
      },
    });
    setCreateOpen(false);
    setPage(0);
    await mergesLoadable.refetch?.();
  };

  return (
    <>
      <Box mb={6}>
        <ListHeader>
          <Typography variant="h5">
            <T keyName="branch_merges_title" />
          </Typography>
          <Button
            color="primary"
            variant="contained"
            startIcon={<Plus width={19} height={19} />}
            onClick={handleCreateMerge}
            data-cy="project-branch-merges-add"
          >
            {t('branch_merges_create_button')}
          </Button>
        </ListHeader>
        <PaginatedHateoasList
          loadable={mergesLoadable as any}
          onPageChange={setPage}
          wrapperComponent="div"
          listComponent={MergeList}
          data-cy="project-branch-merges-list"
          emptyPlaceholder={
            <EmptyPlaceholder>
              <Typography color="textSecondary">
                {t('branch_merges_empty')}
              </Typography>
            </EmptyPlaceholder>
          }
          renderItem={(merge: BranchMergeModel) => (
            <BranchMergeRow
              merge={merge}
              onDelete={() => handleDelete(merge)}
              deleting={deletingId === merge.id}
            />
          )}
          getKey={(merge: BranchMergeModel) => merge.id}
        />
      </Box>
      <BranchMergeCreateModal
        open={createOpen}
        close={() => setCreateOpen(false)}
        submit={handleSubmit}
        saveActionLoadable={createMergeMutation}
      />
    </>
  );
};
