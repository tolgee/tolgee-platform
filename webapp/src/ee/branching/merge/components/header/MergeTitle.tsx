import { T } from '@tolgee/react';
import { BranchNameChipNode } from 'tg.component/branching/BranchNameChip';
import React from 'react';
import { Box } from '@mui/material';
import { BranchMergeModel } from 'tg.ee.module/branching/merge/types';

export const MergeTitle = ({ merge }: { merge: BranchMergeModel }) => {
  return (
    <>
      <Box>
        <T
          keyName="branch_merging_into_title"
          params={{
            branch: <BranchNameChipNode />,
            sourceName: merge.sourceBranchName,
            targetName: merge.targetBranchName,
          }}
        />
      </Box>
    </>
  );
};
