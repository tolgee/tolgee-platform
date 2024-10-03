import React from 'react';
import { Link } from 'react-router-dom';
import { styled } from '@mui/material';
import { useProject } from 'tg.hooks/useProject';
import { TaskTooltip } from 'tg.ee/task/components/TaskTooltip';
import { getTaskRedirect } from 'tg.ee/task/components/utils';

import { TaskReferenceData } from '../types';

const StyledId = styled('span')`
  font-size: 15px;
`;

type Props = {
  data: TaskReferenceData;
};

export const TaskReference: React.FC<Props> = ({ data }) => {
  const project = useProject();

  return (
    <TaskTooltip
      taskNumber={data.number}
      project={project}
      newTaskActions={false}
    >
      <Link
        style={{ textDecoration: 'none' }}
        className="reference"
        to={getTaskRedirect(project, data.number)}
      >
        <span>{data.name} </span>
        <StyledId>#{data.number} </StyledId>
      </Link>
    </TaskTooltip>
  );
};
