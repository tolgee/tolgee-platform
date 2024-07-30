import React from 'react';
import { styled } from '@mui/material';

import { Task } from './types';
import { TaskLabel } from '../TaskLabel';

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
      <TaskLabel task={data} />
    </StyledOrgItem>
  );
};
