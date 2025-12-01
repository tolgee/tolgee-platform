import React from 'react';
import { Alert, Box, MenuItem, styled, Typography } from '@mui/material';
import { DotsVertical } from '@untitled-ui/icons-react';
import { T } from '@tolgee/react';
import { BranchMergeModel } from '../types';
import { StatsBlock } from './StatsBlock';
import { MergeTitle } from 'tg.ee.module/branching/merge/components/header/MergeTitle';
import { IconMenu } from 'tg.component/common/menu/IconMenu';

type Props = {
  merge: BranchMergeModel;
  onDelete: () => void;
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

export const MergeHeader: React.FC<Props> = ({ merge, onDelete }) => {
  const hasUnresolvedConflicts = merge.keyUnresolvedConflictsCount > 0;

  return (
    <>
      <StyledHead>
        <Typography variant="h4">
          <T keyName="branches_merge_title" />
        </Typography>
        <IconMenu
          icon={<DotsVertical />}
          data-cy="branch-merge-detail-menu"
          renderMenu={() => (
            <MenuItem onClick={onDelete}>
              <Typography color={(theme) => theme.palette.error.main}>
                <T keyName="branch_merge_delete_button" />
              </Typography>
            </MenuItem>
          )}
        />
      </StyledHead>

      <StyledBody data-cy="project-branch-merge-detail">
        <MergeTitle merge={merge} />

        <StatsBlock merge={merge} />

        {hasUnresolvedConflicts && (
          <Alert severity="warning">
            <T
              keyName="branch_merges_unresolved_conflicts_alert"
              params={{ value: merge.keyUnresolvedConflictsCount }}
            />
          </Alert>
        )}
      </StyledBody>
    </>
  );
};
