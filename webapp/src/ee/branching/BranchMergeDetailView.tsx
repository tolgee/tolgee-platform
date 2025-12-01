import React, { FC, useEffect, useState } from 'react';
import { Box, Button, CircularProgress, styled } from '@mui/material';
import { T, useTranslate } from '@tolgee/react';
import { useHistory, useParams } from 'react-router-dom';

import { LINKS, PARAMS } from 'tg.constants/links';
import { useProject } from 'tg.hooks/useProject';
import { useMergeTabs } from './merge/hooks/useMergeTabs';
import { useMergeData } from './merge/hooks/useMergeData';
import { MergeHeader } from './merge/components/MergeHeader';
import { ChangesTabs } from './merge/components/ChangesTabs';
import { ChangeList } from './merge/components/ChangeList';
import { BranchMergeChangeType, BranchMergeConflictModel } from './merge/types';
import { BaseProjectView } from 'tg.views/projects/BaseProjectView';
import { useMessage } from 'tg.hooks/useSuccessMessage';

type RouteParams = {
  mergeId: string;
};

const StyledDetail = styled(Box)`
  display: flex;
  flex-direction: column;
  gap: ${({ theme }) => theme.spacing(2)};
`;

export const BranchMergeDetailView: FC = () => {
  const { t } = useTranslate();
  const project = useProject();
  const messaging = useMessage();
  const history = useHistory();
  const { mergeId } = useParams<RouteParams>();
  const numericMergeId = Number(mergeId);

  const [selectedTab, setSelectedTab] =
    useState<BranchMergeChangeType>('CONFLICT');

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

  const handleApply = async () => {
    await applyMutation.mutateAsync({
      path: { projectId: project.id, mergeId: numericMergeId },
    });
    messaging.success(<T keyName="branch_merges_apply_success" />);
    history.push(
      LINKS.PROJECT_TRANSLATIONS_WITH_BRANCH.build({
        [PARAMS.PROJECT_ID]: project.id,
        [PARAMS.BRANCH]: merge!.targetBranchName,
      })
    );
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

  const readyToMerge =
    merge?.keyUnresolvedConflictsCount === 0 && merge?.outdated === false;

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

            <ChangesTabs
              tabs={tabs}
              selectedTab={selectedTab ?? 'CONFLICT'}
              onSelect={setSelectedTab}
            />

            <Box display="grid" rowGap={2}>
              <ChangeList
                merge={merge}
                changes={changes}
                selectedTab={selectedTab ?? 'CONFLICT'}
                isLoading={changesLoadable.isLoading}
                onResolve={handleResolve}
                onResolveAll={handleResolveAll}
                resolveAllLoading={resolveAllMutation.isLoading}
              />
              <Box display="flex" justifyContent="end" columnGap={2}>
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
            </Box>
          </StyledDetail>
        )
      )}
    </BaseProjectView>
  );
};
