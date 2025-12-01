import {
  Box,
  Button,
  CircularProgress,
  styled,
  Typography,
} from '@mui/material';
import { T } from '@tolgee/react';
import { Branch, CheckDone } from 'tg.component/CustomIcons';

import {
  BranchMergeChangeModel,
  BranchMergeChangeType,
  BranchMergeConflictModel,
  BranchMergeModel,
} from '../types';
import { ConflictKeyPanel, SingleKeyPanel } from './KeyPanels';

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
  const showSourceHeader = selectedTab !== 'DELETE' && selectedTab !== 'ADD';

  return (
    <Box>
      {isLoading ? (
        <CircularProgress />
      ) : (
        changes.length && (
          <ConflictsWrapper>
            <ConflictsHeader>
              {showSourceHeader && (
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
              )}
              <ConflictsHeaderColumn>
                <Box display="flex" gap={1}>
                  <Branch height={18} width={18} />
                  <Typography variant="body2" fontWeight="medium">
                    {selectedTab === 'ADD'
                      ? merge.sourceBranchName
                      : merge.targetBranchName}
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
              const showTwoColumns = isConflict || isModification;
              const acceptedSource = change.resolution === 'SOURCE';
              const acceptedTarget = change.resolution === 'TARGET';

              return (
                <ChangeCard
                  key={change.id}
                  data-cy="project-branch-merge-change"
                >
                  {showTwoColumns ? (
                    <ConflictColumns>
                      {sourceKey && (
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
                      )}
                      {targetKey && (
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
                      )}
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
