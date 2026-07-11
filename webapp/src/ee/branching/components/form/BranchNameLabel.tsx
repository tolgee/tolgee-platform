import { T } from '@tolgee/react';
import { LabelHint } from 'tg.component/common/LabelHint';
import React from 'react';
import { Box, styled } from '@mui/material';

const StyledHint = styled(Box)`
  ul {
    margin: 0;
    padding-left: 16px;
  }
`;

export const BranchNameLabel = () => (
  <LabelHint
    title={
      <StyledHint>
        <T keyName="branch_name_rules" params={{ ul: <ul />, li: <li /> }} />
      </StyledHint>
    }
  >
    <T keyName="project_branch_name" />
  </LabelHint>
);
