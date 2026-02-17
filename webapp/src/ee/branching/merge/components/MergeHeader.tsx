import React from 'react';
import { Alert, Box, styled, Typography } from '@mui/material';
import { T } from '@tolgee/react';
import { BranchMergeModel } from '../types';
import { StatsBlock } from './header/StatsBlock';
import { MergeTitle } from 'tg.ee.module/branching/merge/components/header/MergeTitle';

type Props = {
  merge: BranchMergeModel;
};

const StyledHead = styled(Box)`
  display: flex;
  justify-content: space-between;
  margin-bottom: ${({ theme }) => theme.spacing(2)};
  align-items: center;
`;

const StyledBody = styled(Box)`
  display: flex;
  flex-direction: column;
  gap: ${({ theme }) => theme.spacing(2)};
`;

export const MergeHeader: React.FC<Props> = ({ merge }) => {
  const hasUnresolvedConflicts = merge.keyUnresolvedConflictsCount > 0;

  return (
    <>
      <StyledHead>
        <Typography variant="h4">
          <T keyName="branches_merge_title" />
        </Typography>
      </StyledHead>

      <StyledBody data-cy="project-branch-merge-detail">
        <MergeTitle merge={merge} />

        {hasUnresolvedConflicts && (
          <Alert severity="warning">
            <T
              keyName="branch_merges_unresolved_conflicts_alert"
              params={{ value: merge.keyUnresolvedConflictsCount, b: <b /> }}
            />
          </Alert>
        )}

        {merge.uncompletedTasksCount > 0 && (
          <Alert severity="warning">
            <T
              keyName="branch_merges_uncompleted_tasks_alert"
              params={{ value: merge.uncompletedTasksCount, b: <b /> }}
            />
          </Alert>
        )}

        <StatsBlock merge={merge} />
      </StyledBody>
    </>
  );
};
