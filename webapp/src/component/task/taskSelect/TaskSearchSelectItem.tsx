import React from 'react';
import { Box, styled } from '@mui/material';

import { Task } from './types';

const StyledOrgItem = styled('div')`
  display: grid;
  grid-auto-flow: column;
  gap: 6px;
  align-items: center;
  text: ${({ theme }) => theme.palette.primaryText};
`;

type Props = {
  data: Task;
};

export const TaskSearchSelectItem: React.FC<Props> = ({ data }) => {
  return (
    <StyledOrgItem>
      <Box>{data.name}</Box>
    </StyledOrgItem>
  );
};
