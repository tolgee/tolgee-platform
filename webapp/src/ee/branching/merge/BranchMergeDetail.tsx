import React, { FC, useEffect, useRef, useState } from 'react';
import {
  Box,
  Button,
  Checkbox,
  CircularProgress,
  FormControlLabel,
  Portal,
  styled,
  Alert,
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
import { BranchMergeChangeType, BranchMergeConflictModel } from './types';
import { BaseProjectView } from 'tg.views/projects/BaseProjectView';
import { useMessage } from 'tg.hooks/useSuccessMessage';
import { MENU_WIDTH } from 'tg.views/projects/projectMenu/SideMenu';
import { confirmation } from 'tg.hooks/confirmation';

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

export const BranchMergeDetail: FC = () => {
  const { t } = useTranslate();
  const project = useProject();
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
  const [deleteBranchAfterMerge, setDeleteBranchAfterMerge] = useState(true);

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
    applyMutation,
    deleteMutation,
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
    if (merge.outdated && !previewLoadable.isLoading) {
      refreshPreviewMutation.mutate({
        path: { projectId: project.id, mergeId: numericMergeId },
      });
    }
  }, [merge?.outdated]);

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
    await Promise.all([
      changesLoadable.refetch?.(),
      previewLoadable.refetch?.(),
    ]);
  };

  const applyMerge = async () => {
    await applyMutation.mutateAsync({
      path: { projectId: project.id, mergeId: numericMergeId },
      content: {
        'application/json': { deleteBranch: deleteBranchAfterMerge },
      },
    });
    messaging.success(<T keyName="branch_merges_apply_success" />);
    history.push(
      LINKS.PROJECT_TRANSLATIONS_WITH_BRANCH.build({
        [PARAMS.PROJECT_ID]: project.id,
        [PARAMS.BRANCH]: merge!.targetBranchName,
      })
    );
  };

  const handleApply = async () => {
    if ((merge?.uncompletedTasksCount ?? 0) > 0) {
      confirmation({
        message: (
          <T
            keyName="branch_merges_uncompleted_tasks_confirmation"
            params={{ value: merge?.uncompletedTasksCount }}
          />
        ),
        confirmButtonText: <T keyName="branch_merges_apply_button" />,
        onConfirm: applyMerge,
      });
      return;
    }
    await applyMerge();
  };

  const handleCancel = async () => {
    await deleteMutation.mutateAsync({
      path: { projectId: project.id, mergeId: numericMergeId },
    });
    history.push(
      LINKS.PROJECT_BRANCHES.build({
        [PARAMS.PROJECT_ID]: project.id,
      })
    );
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

  const readyToMerge =
    totalChanges > 0 &&
    merge?.keyUnresolvedConflictsCount === 0 &&
    merge?.outdated === false;

  const actionControls = (
    <>
      <FormControlLabel
        control={
          <Checkbox
            size="small"
            checked={deleteBranchAfterMerge}
            onChange={(event) =>
              setDeleteBranchAfterMerge(event.target.checked)
            }
          />
        }
        label={
          <T
            keyName="branch_merge_delete_branch_after_merge"
            params={{ b: <b />, branch: merge?.sourceBranchName }}
          />
        }
      />
      <Box display="flex" columnGap={2}>
        <Button
          variant="outlined"
          color="primary"
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
          onClick={handleApply}
          disabled={!readyToMerge || applyMutation.isLoading}
        >
          <T keyName="branch_merges_apply_button" />
        </Button>
      </Box>
    </>
  );

  return (
    <BaseProjectView
      maxWidth={1200}
      windowTitle={t('branch_merges_title')}
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
            <MergeHeader merge={merge} onDelete={handleCancel} />

            {merge.uncompletedTasksCount > 0 && (
              <Alert severity="warning">
                <T
                  keyName="branch_merges_uncompleted_tasks_alert"
                  params={{ value: merge.uncompletedTasksCount }}
                />
              </Alert>
            )}

            {totalChanges > 0 && (
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
              />
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
    </BaseProjectView>
  );
};
