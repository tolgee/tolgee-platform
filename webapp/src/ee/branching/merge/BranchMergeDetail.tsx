import React, { FC, useEffect, useRef, useState } from 'react';
import { useQueryClient } from 'react-query';
import {
  Box,
  Button,
  Checkbox,
  CircularProgress,
  FormControlLabel,
  Portal,
  styled,
  Alert,
  Typography,
} from '@mui/material';
import { T, useTranslate } from '@tolgee/react';
import { useHistory, useParams } from 'react-router-dom';
import { useInView } from 'react-intersection-observer';

import { LINKS, PARAMS } from 'tg.constants/links';
import { useProject } from 'tg.hooks/useProject';
import { useMergeTabs } from './hooks/useMergeTabs';
import { useMergeData } from './hooks/useMergeData';
import { MergeHeader } from './components/MergeHeader';
import { ChangesTabs } from './components/changes/ChangesTabs';
import { ChangeList } from './components/changes/ChangeList';
import {
  BranchMergeChangeModel,
  BranchMergeChangeType,
  BranchMergeConflictModel,
} from './types';
import { BaseProjectView } from 'tg.views/projects/BaseProjectView';
import { useMessage } from 'tg.hooks/useSuccessMessage';
import { MENU_WIDTH } from 'tg.views/projects/projectMenu/SideMenu';
import { confirmation } from 'tg.hooks/confirmation';
import { BranchProgressModal } from '../components/BranchProgressModal';
import { apiSchemaHttpService } from 'tg.service/http/ApiSchemaHttpService';

type RouteParams = {
  mergeId: string;
};

const StyledDetail = styled(Box)`
  display: flex;
  flex-direction: column;
  gap: ${({ theme }) => theme.spacing(2)};
`;

const StyledFloatingActions = styled(Box)`
  position: fixed;
  bottom: 0;
  left: 0;
  width: 100%;
  z-index: ${({ theme }) => theme.zIndex.drawer};
  display: flex;
  justify-content: center;
  pointer-events: none;
`;

const StyledFloatingActionsInner = styled(Box)`
  pointer-events: all;
  background: ${({ theme }) => theme.palette.background.paper};
  box-shadow: 0 -6px 12px rgba(0, 0, 0, 0.15);
  border-radius: ${({ theme }) => theme.shape.borderRadius}px;
`;

export const BranchMergeDetail: FC<React.PropsWithChildren<unknown>> = () => {
  const { t } = useTranslate();
  const project = useProject();
  const queryClient = useQueryClient();
  const messaging = useMessage();
  const history = useHistory();
  const { mergeId } = useParams<RouteParams>();
  const numericMergeId = Number(mergeId);
  const userSelectedTab = useRef(false);
  const { ref: actionsRef, inView: actionsInView } = useInView({
    rootMargin: '0px',
    threshold: 0.1,
  });

  const [selectedTab, setSelectedTab] =
    useState<BranchMergeChangeType>('CONFLICT');
  const [deleteBranchAfterMerge, setDeleteBranchAfterMerge] = useState(false);

  const labels = {
    ADD: t('branch_merges_additions'),
    UPDATE: t('branch_merges_modifications'),
    DELETE: t('branch_merges_deletions'),
    CONFLICT: t('branch_merges_conflicts_title'),
  } as const;

  const {
    previewLoadable,
    merge,
    changesLoadable,
    changes,
    resolveMutation,
    resolveAllMutation,
    excludeMutation,
    applyMutation,
    refreshPreviewMutation,
  } = useMergeData(project.id, numericMergeId, selectedTab);

  const { tabs } = useMergeTabs(merge, labels, selectedTab, setSelectedTab);

  useEffect(() => {
    userSelectedTab.current = false;
    setSelectedTab('CONFLICT');
  }, [numericMergeId]);

  useEffect(() => {
    if (!merge || userSelectedTab.current) {
      return;
    }

    const totalConflicts =
      merge.keyResolvedConflictsCount + merge.keyUnresolvedConflictsCount;

    const firstTabWithData: BranchMergeChangeType | undefined = [
      ['CONFLICT', totalConflicts],
      ['ADD', merge.keyAdditionsCount],
      ['UPDATE', merge.keyModificationsCount],
      ['DELETE', merge.keyDeletionsCount],
    ].find(([, count]) => (count as number) > 0)?.[0] as BranchMergeChangeType;

    if (firstTabWithData && firstTabWithData !== selectedTab) {
      setSelectedTab(firstTabWithData);
    }
  }, [merge, selectedTab]);

  useEffect(() => {
    if (!merge) return;
    if (
      merge.outdated &&
      !previewLoadable.isLoading &&
      !refreshPreviewMutation.isLoading
    ) {
      refreshPreviewMutation
        .mutateAsync({
          path: { projectId: project.id, mergeId: numericMergeId },
        })
        .then(() => {
          Promise.all([
            previewLoadable.refetch?.(),
            changesLoadable.refetch?.(),
          ]);
        });
    }
  }, [merge?.outdated]);

  const refetchChange = async (changeId: number) => {
    const updatedChange = await apiSchemaHttpService.schemaRequest(
      '/v2/projects/{projectId}/branches/merge/{mergeId}/changes/{changeId}',
      'get'
    )({
      path: {
        projectId: project.id,
        mergeId: numericMergeId,
        changeId,
      },
    });

    const queryKey = [
      '/v2/projects/{projectId}/branches/merge/{mergeId}/changes',
      { projectId: project.id, mergeId: numericMergeId },
      { size: 10, type: selectedTab },
    ];

    queryClient.setQueryData(queryKey, (oldData: any) => {
      if (!oldData?.pages) return oldData;
      return {
        ...oldData,
        pages: oldData.pages.map((page: any) => ({
          ...page,
          _embedded: {
            ...page._embedded,
            branchMergeChanges: page._embedded?.branchMergeChanges?.map(
              (change: any) => (change.id === changeId ? updatedChange : change)
            ),
          },
        })),
      };
    });

    await previewLoadable.refetch?.();
  };

  const handleResolve = async (
    conflict: BranchMergeConflictModel,
    resolution: 'SOURCE' | 'TARGET'
  ) => {
    await resolveMutation.mutateAsync({
      path: { projectId: project.id, mergeId: numericMergeId },
      content: {
        'application/json': {
          changeId: conflict.id,
          resolve: resolution,
        },
      },
    });
    await refetchChange(conflict.id);
  };

  const handleSetExcluded = async (
    change: BranchMergeChangeModel,
    excluded: boolean
  ) => {
    userSelectedTab.current = true;
    await excludeMutation.mutateAsync({
      path: { projectId: project.id, mergeId: numericMergeId },
      content: {
        'application/json': { changeIds: [change.id], excluded },
      },
    });
    await refetchChange(change.id);
  };

  const handleSetTypeExcluded = async (
    type: BranchMergeChangeType,
    excluded: boolean
  ) => {
    userSelectedTab.current = true;
    await excludeMutation.mutateAsync({
      path: { projectId: project.id, mergeId: numericMergeId },
      content: { 'application/json': { type, excluded } },
    });
    await Promise.all([
      changesLoadable.refetch?.(),
      previewLoadable.refetch?.(),
    ]);
  };

  const handleResolveAll = async (resolution: 'SOURCE' | 'TARGET') => {
    await resolveAllMutation.mutateAsync({
      path: { projectId: project.id, mergeId: numericMergeId },
      content: {
        'application/json': {
          resolve: resolution,
        },
      },
    });
    // For resolve all, we need to refetch all conflicts since every item is updated
    await Promise.all([
      changesLoadable.refetch?.(),
      previewLoadable.refetch?.(),
    ]);
  };

  const applyMerge = async () => {
    await applyMutation.mutateAsync({
      path: { projectId: project.id, mergeId: numericMergeId },
      content: {
        'application/json': {
          deleteBranch: merge?.isUpdate ? false : deleteBranchAfterMerge,
        },
      },
    });
    messaging.success(
      merge?.isUpdate ? (
        <T
          keyName="branch_update_apply_success"
          defaultValue="Branch successfully updated"
        />
      ) : (
        <T keyName="branch_merges_apply_success" />
      )
    );
    history.push(
      LINKS.PROJECT_TRANSLATIONS_WITH_BRANCH.build({
        [PARAMS.PROJECT_ID]: project.id,
        [PARAMS.BRANCH]: merge!.targetBranchName,
      })
    );
  };

  const handleApply = async () => {
    if (
      !merge?.isUpdate &&
      deleteBranchAfterMerge &&
      (merge?.uncompletedTasksCount ?? 0) > 0
    ) {
      confirmation({
        message: (
          <T
            keyName="branch_merges_uncompleted_tasks_confirmation"
            params={{ value: merge?.uncompletedTasksCount, b: <b /> }}
          />
        ),
        title: (
          <T keyName="branch_merges_uncompleted_tasks_confirmation_title" />
        ),
        confirmButtonText: <T keyName="branch_merges_apply_button" />,
        onConfirm: applyMerge,
      });
      return;
    }
    await applyMerge();
  };

  const handleTabSelect = (tab: BranchMergeChangeType) => {
    userSelectedTab.current = true;
    setSelectedTab(tab);
  };

  const totalChanges = merge
    ? merge.keyAdditionsCount +
      merge.keyModificationsCount +
      merge.keyDeletionsCount +
      merge.keyUnresolvedConflictsCount +
      merge.keyResolvedConflictsCount
    : 0;

  const totalWithExcluded = totalChanges + (merge?.keyExcludedCount ?? 0);

  const isOutdated = merge?.outdated;

  const readyToMerge =
    totalChanges > 0 && merge?.keyUnresolvedConflictsCount === 0 && !isOutdated;

  const actionControls = (
    <>
      {!merge?.isUpdate && (
        <FormControlLabel
          control={
            <Checkbox
              size="small"
              checked={deleteBranchAfterMerge}
              onChange={(event) =>
                setDeleteBranchAfterMerge(event.target.checked)
              }
              data-cy="project-branch-merge-delete-branch-checkbox"
            />
          }
          label={
            <T
              keyName="branch_merge_delete_branch_after_merge"
              params={{ b: <b />, branch: merge?.sourceBranchName }}
            />
          }
        />
      )}
      <Box display="flex" columnGap={2} alignItems="center">
        <Typography
          color="textSecondary"
          data-cy="project-branch-merge-included-count"
          data-cy-included={totalChanges}
          data-cy-total={totalWithExcluded}
        >
          <T
            keyName="branch_merge_included_count"
            defaultValue="{total, plural, one {{included} of {total} key included} other {{included} of {total} keys included}}"
            params={{ included: totalChanges, total: totalWithExcluded }}
          />
        </Typography>
        <Button
          variant="outlined"
          color="primary"
          data-cy="project-branch-merge-cancel"
          onClick={() =>
            history.push(
              LINKS.PROJECT_BRANCHES.build({
                [PARAMS.PROJECT_ID]: project.id,
              })
            )
          }
        >
          <T keyName="branch_merge_cancel_button" />
        </Button>
        <Button
          variant="contained"
          color="primary"
          data-cy="project-branch-merge-apply"
          onClick={handleApply}
          disabled={!readyToMerge || applyMutation.isLoading}
        >
          {merge?.isUpdate ? (
            <T
              keyName="branch_update_apply_button"
              defaultValue="Update branch"
            />
          ) : (
            <T keyName="branch_merges_apply_button" />
          )}
        </Button>
      </Box>
    </>
  );

  return (
    <BaseProjectView
      maxWidth={1200}
      windowTitle={
        merge?.isUpdate
          ? t('branches_update_title', 'Update branch')
          : t('branch_merges_title')
      }
      navigation={[
        [
          t('branches_title'),
          LINKS.PROJECT_BRANCHES.build({ [PARAMS.PROJECT_ID]: project.id }),
        ],
      ]}
    >
      {previewLoadable.isLoading ? (
        <CircularProgress />
      ) : (
        merge && (
          <StyledDetail>
            <MergeHeader merge={merge} />

            {isOutdated && (
              <Box mt={1}>
                <Alert severity="warning">
                  <T keyName="branch_merges_status_outdated" />
                </Alert>
              </Box>
            )}

            {totalWithExcluded > 0 && (
              <ChangesTabs
                tabs={tabs}
                selectedTab={selectedTab ?? 'CONFLICT'}
                onSelect={handleTabSelect}
              />
            )}

            <Box display="grid" rowGap={2}>
              <ChangeList
                merge={merge}
                changes={changes}
                selectedTab={selectedTab ?? 'CONFLICT'}
                isLoading={changesLoadable.isLoading}
                hasNextPage={changesLoadable.hasNextPage}
                isFetchingNextPage={changesLoadable.isFetchingNextPage}
                onLoadMore={changesLoadable.fetchNextPage}
                onResolve={handleResolve}
                onResolveAll={handleResolveAll}
                resolveAllLoading={resolveAllMutation.isLoading}
                onSetExcluded={handleSetExcluded}
                onSetTypeExcluded={handleSetTypeExcluded}
              />
              <Alert severity="info">
                {t('branch_merge_tags_labels_screenshots_joined')}{' '}
                {merge.isUpdate ? (
                  <T
                    keyName="branch_update_excluded_stay_pending"
                    defaultValue="Keys you exclude stay pending for the next update."
                  />
                ) : (
                  <T
                    keyName="branch_merge_excluded_stay_pending"
                    defaultValue="Keys you exclude stay pending for the next merge."
                  />
                )}
              </Alert>
              <Box
                ref={actionsRef}
                display="flex"
                justifyContent="space-between"
                alignItems="center"
              >
                {actionControls}
              </Box>
            </Box>
          </StyledDetail>
        )
      )}
      {!actionsInView && merge && (
        <Portal>
          <StyledFloatingActions
            style={{
              left: MENU_WIDTH / 2,
            }}
          >
            <StyledFloatingActionsInner
              display="flex"
              justifyContent="space-between"
              alignItems="center"
              maxWidth={1200}
              mx="auto"
              width="100%"
              px={2}
              py={1.5}
            >
              {actionControls}
            </StyledFloatingActionsInner>
          </StyledFloatingActions>
        </Portal>
      )}
      <BranchProgressModal
        open={applyMutation.isLoading}
        title={
          merge?.isUpdate ? (
            <T
              keyName="branch_update_applying_title"
              defaultValue="Updating branch"
            />
          ) : (
            <T keyName="branch_merge_applying_title" />
          )
        }
        message={
          merge?.isUpdate ? (
            <T
              keyName="branch_update_applying_message"
              defaultValue="Please wait while the branch is being updated..."
            />
          ) : (
            <T keyName="branch_merge_applying_message" />
          )
        }
      />
      <BranchProgressModal
        open={refreshPreviewMutation.isLoading}
        title={<T keyName="branch_merge_refreshing_title" />}
        message={<T keyName="branch_merge_refreshing_message" />}
      />
    </BaseProjectView>
  );
};
