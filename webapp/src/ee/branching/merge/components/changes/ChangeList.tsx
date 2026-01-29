import {
  Box,
  Button,
  CircularProgress,
  styled,
  Typography,
} from '@mui/material';
import { useEffect, useState } from 'react';
import { T, useTranslate } from '@tolgee/react';
import { useInView } from 'react-intersection-observer';
import { Branch, CheckDone } from 'tg.component/CustomIcons';

import {
  BranchMergeChangeModel,
  BranchMergeChangeType,
  BranchMergeConflictModel,
  BranchMergeModel,
} from '../../types';
import { ConflictKeyPanel } from './ConflictKeyPanel';
import { PlaceholderKeyPanel } from './PlaceholderKeyPanel';
import { SingleKeyPanel } from './SingleKeyPanel';

const ConflictsWrapper = styled(Box)`
  display: grid;
  gap: ${({ theme }) => theme.spacing(2)};
`;

const ConflictsHeader = styled(Box)`
  display: flex;
  gap: ${({ theme }) => theme.spacing(2)};
`;

const ConflictsHeaderColumn = styled(Box)`
  display: flex;
  flex: 1;
  gap: 10px;
  align-items: center;
  justify-content: space-between;
`;

const CenteredConflictsHeaderColumn = styled(ConflictsHeaderColumn)`
  justify-content: center;
`;

const ConflictColumns = styled(Box)`
  display: flex;
  gap: ${({ theme }) => theme.spacing(2)};
`;

const ChangeCard = styled(Box)`
  display: grid;
  gap: ${({ theme }) => theme.spacing(2)};
`;

type Props = {
  merge: BranchMergeModel;
  changes: BranchMergeChangeModel[];
  selectedTab: BranchMergeChangeType;
  isLoading: boolean;
  hasNextPage?: boolean;
  isFetchingNextPage?: boolean;
  onLoadMore?: () => void;
  onResolve?: (
    conflict: BranchMergeConflictModel,
    resolution: 'SOURCE' | 'TARGET'
  ) => void;
  onResolveAll?: (resolution: 'SOURCE' | 'TARGET') => void;
  resolveAllLoading?: boolean;
};

export const ChangeList = ({
  merge,
  changes,
  selectedTab,
  isLoading,
  hasNextPage,
  isFetchingNextPage,
  onLoadMore,
  onResolve,
  onResolveAll,
  resolveAllLoading,
}: Props) => {
  const { t } = useTranslate();
  const [showAllMap, setShowAllMap] = useState<Record<string, boolean>>({});
  const { ref, inView } = useInView({ rootMargin: '200px' });

  useEffect(() => {
    if (inView && hasNextPage && !isFetchingNextPage) {
      onLoadMore?.();
    }
  }, [inView, hasNextPage, isFetchingNextPage, onLoadMore]);

  const toggleShowAll = (changeId: number) => {
    setShowAllMap((prev) => ({ ...prev, [changeId]: !prev[changeId] }));
  };

  const showMergedColumn =
    selectedTab === 'CONFLICT' || selectedTab === 'UPDATE';

  return (
    <Box>
      {isLoading ? (
        <CircularProgress />
      ) : (
        changes.length > 0 && (
          <ConflictsWrapper>
            <ConflictsHeader>
              <ConflictsHeaderColumn>
                <Box display="flex" gap={1}>
                  <Branch height={18} width={18} />
                  <Typography variant="body2" fontWeight="medium">
                    {merge.sourceBranchName}
                  </Typography>
                </Box>
                {merge.mergedAt == null &&
                  selectedTab === 'CONFLICT' &&
                  onResolveAll &&
                  changes.length > 1 && (
                    <Button
                      size="small"
                      variant="outlined"
                      onClick={() => onResolveAll('SOURCE')}
                      disabled={resolveAllLoading}
                      endIcon={<CheckDone />}
                    >
                      <T keyName="branch_merges_accept_all" />
                    </Button>
                  )}
              </ConflictsHeaderColumn>
              {showMergedColumn && (
                <CenteredConflictsHeaderColumn>
                  <Typography variant="body2" fontWeight="medium">
                    <T keyName="branch_merges_merged_column" />
                  </Typography>
                </CenteredConflictsHeaderColumn>
              )}
              <ConflictsHeaderColumn>
                <Box display="flex" gap={1}>
                  <Branch height={18} width={18} />
                  <Typography variant="body2" fontWeight="medium">
                    {merge.targetBranchName}
                  </Typography>
                </Box>
                {merge.mergedAt == null &&
                  selectedTab === 'CONFLICT' &&
                  onResolveAll &&
                  changes.length > 1 && (
                    <Button
                      size="small"
                      variant="outlined"
                      onClick={() => onResolveAll('TARGET')}
                      disabled={resolveAllLoading}
                      endIcon={<CheckDone />}
                    >
                      <T keyName="branch_merges_accept_all" />
                    </Button>
                  )}
              </ConflictsHeaderColumn>
            </ConflictsHeader>

            {changes.map((change) => {
              const sourceKey = change.sourceKey;
              const mergedKey = change.mergedKey;
              const targetKey = change.targetKey;
              const isConflict = change.type === 'CONFLICT';
              const isModification = change.type === 'UPDATE';
              const isAddition = change.type === 'ADD';
              const isDeletion = change.type === 'DELETE';
              const showThreeColumns =
                showMergedColumn && (isConflict || isModification);
              const showTwoColumns = isAddition || isDeletion;
              const acceptedSource = change.resolution === 'SOURCE';
              const acceptedTarget = change.resolution === 'TARGET';
              const showAll = showAllMap[change.id] ?? false;
              const toggleLabels = {
                showAll: t('branch_merge_show_translations'),
                showLess: t('branch_merge_hide_translations'),
              };
              const changedTranslations = change.changedTranslations ?? [];

              const additionPlaceholder = (
                <PlaceholderKeyPanel text={t('branch_merge_placeholder_add')} />
              );
              const deletionPlaceholder = (
                <PlaceholderKeyPanel text={t('branch_merge_placeholder_del')} />
              );
              const unresolvedMergedPlaceholder = (
                <PlaceholderKeyPanel
                  text={t('branch_merge_placeholder_merged')}
                />
              );

              let leftPanel: JSX.Element | null = null;
              let middlePanel: JSX.Element | null = null;
              let rightPanel: JSX.Element | null = null;

              if (isConflict || isModification) {
                leftPanel = sourceKey ? (
                  <ConflictKeyPanel
                    keyData={sourceKey}
                    accepted={isConflict ? acceptedSource : undefined}
                    changedTranslations={changedTranslations}
                    showAll={showAll}
                    onToggleShowAll={() => toggleShowAll(change.id)}
                    onAccept={
                      isConflict && merge.mergedAt == null && onResolve
                        ? () =>
                            onResolve(
                              change as BranchMergeConflictModel,
                              'SOURCE'
                            )
                        : undefined
                    }
                  />
                ) : null;

                middlePanel = mergedKey ? (
                  <SingleKeyPanel
                    keyData={mergedKey}
                    changedTranslations={changedTranslations}
                    showAll={showAll}
                    onToggleShowAll={() => toggleShowAll(change.id)}
                  />
                ) : (
                  unresolvedMergedPlaceholder
                );

                rightPanel = targetKey ? (
                  <ConflictKeyPanel
                    keyData={targetKey}
                    accepted={isConflict ? acceptedTarget : undefined}
                    changedTranslations={changedTranslations}
                    showAll={showAll}
                    onToggleShowAll={() => toggleShowAll(change.id)}
                    onAccept={
                      isConflict && merge.mergedAt == null && onResolve
                        ? () =>
                            onResolve(
                              change as BranchMergeConflictModel,
                              'TARGET'
                            )
                        : undefined
                    }
                  />
                ) : null;
              } else if (isAddition) {
                leftPanel = sourceKey ? (
                  <SingleKeyPanel
                    keyData={sourceKey}
                    showAll={showAll}
                    onToggleShowAll={() => toggleShowAll(change.id)}
                    hideAllWhenFalse
                    toggleLabels={toggleLabels}
                    variant="added"
                  />
                ) : (
                  additionPlaceholder
                );
                rightPanel = additionPlaceholder;
              } else if (isDeletion) {
                leftPanel = deletionPlaceholder;
                rightPanel = targetKey ? (
                  <SingleKeyPanel
                    keyData={targetKey}
                    showAll={showAll}
                    onToggleShowAll={() => toggleShowAll(change.id)}
                    hideAllWhenFalse
                    toggleLabels={toggleLabels}
                    variant="deleted"
                  />
                ) : (
                  deletionPlaceholder
                );
              }

              return (
                <ChangeCard
                  key={change.id}
                  data-cy="project-branch-merge-change"
                >
                  {showThreeColumns ? (
                    <ConflictColumns>
                      {leftPanel}
                      {middlePanel}
                      {rightPanel}
                    </ConflictColumns>
                  ) : showTwoColumns ? (
                    <ConflictColumns>
                      {leftPanel}
                      {rightPanel}
                    </ConflictColumns>
                  ) : (
                    <SingleKeyPanel keyData={(sourceKey || targetKey)!} />
                  )}
                </ChangeCard>
              );
            })}
          </ConflictsWrapper>
        )
      )}
      {hasNextPage && (
        <Box ref={ref} display="flex" justifyContent="center" py={2}>
          {isFetchingNextPage && <CircularProgress size={24} />}
        </Box>
      )}
    </Box>
  );
};
