import {
  Box,
  Button,
  CircularProgress,
  styled,
  Typography,
} from '@mui/material';
import { T, useTranslate } from '@tolgee/react';
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
`;

const ConflictsHeaderColumn = styled(Box)`
  display: flex;
  flex: 1;
  gap: 10px;
  align-items: center;
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
  onResolve,
  onResolveAll,
  resolveAllLoading,
}: Props) => {
  const { t } = useTranslate();
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
                  onResolveAll && (
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
              <ConflictsHeaderColumn>
                <Box display="flex" gap={1}>
                  <Branch height={18} width={18} />
                  <Typography variant="body2" fontWeight="medium">
                    {merge.targetBranchName}
                  </Typography>
                </Box>
                {merge.mergedAt == null &&
                  selectedTab === 'CONFLICT' &&
                  onResolveAll && (
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
              const targetKey = change.targetKey;
              const isConflict = change.type === 'CONFLICT';
              const isModification = change.type === 'UPDATE';
              const isAddition = change.type === 'ADD';
              const isDeletion = change.type === 'DELETE';
              const showTwoColumns =
                isConflict || isModification || isAddition || isDeletion;
              const acceptedSource = change.resolution === 'SOURCE';
              const acceptedTarget = change.resolution === 'TARGET';

              const additionPlaceholder = (
                <PlaceholderKeyPanel text={t('branch_merge_placeholder_add')} />
              );
              const deletionPlaceholder = (
                <PlaceholderKeyPanel text={t('branch_merge_placeholder_del')} />
              );

              let leftPanel: JSX.Element | null = null;
              let rightPanel: JSX.Element | null = null;

              if (isConflict || isModification) {
                leftPanel = sourceKey ? (
                  <ConflictKeyPanel
                    keyData={sourceKey}
                    accepted={isConflict ? acceptedSource : undefined}
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

                rightPanel = targetKey ? (
                  <ConflictKeyPanel
                    keyData={targetKey}
                    accepted={isConflict ? acceptedTarget : undefined}
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
                  <SingleKeyPanel keyData={sourceKey} />
                ) : (
                  additionPlaceholder
                );
                rightPanel = additionPlaceholder;
              } else if (isDeletion) {
                leftPanel = deletionPlaceholder;
                rightPanel = targetKey ? (
                  <SingleKeyPanel keyData={targetKey} />
                ) : (
                  deletionPlaceholder
                );
              }

              return (
                <ChangeCard
                  key={change.id}
                  data-cy="project-branch-merge-change"
                >
                  {showTwoColumns ? (
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
    </Box>
  );
};
