import { T } from '@tolgee/react';
import { BranchNameChipNode } from 'tg.component/branching/BranchNameChip';
import React from 'react';
import { Box } from '@mui/material';
import { BranchMergeModel } from 'tg.ee.module/branching/merge/types';

export const MergeTitle = ({ merge }: { merge: BranchMergeModel }) => {
  return (
    <>
      <Box>
        {merge.isUpdate ? (
          <T
            keyName="branch_updating_from_title"
            defaultValue="Updating <branch>{targetName}</branch> from <branch>{sourceName}</branch>"
            params={{
              branch: <BranchNameChipNode />,
              sourceName: merge.sourceBranchName,
              targetName: merge.targetBranchName,
            }}
          />
        ) : (
          <T
            keyName="branch_merging_into_title"
            params={{
              branch: <BranchNameChipNode />,
              sourceName: merge.sourceBranchName,
              targetName: merge.targetBranchName,
            }}
          />
        )}
      </Box>
    </>
  );
};
