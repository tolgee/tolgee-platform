import React from 'react';
import { Link } from 'react-router-dom';
import { styled } from '@mui/material';
import { useProject } from 'tg.hooks/useProject';

import { TaskTooltip } from './TaskTooltip';
import { getTaskUrl } from 'tg.constants/links';
import { TaskReferenceProps } from '../../../eeSetup/EeModuleType';

const StyledId = styled('span')`
  font-size: 15px;
`;

export const TaskReference: React.FC<TaskReferenceProps> = ({ data }) => {
  const project = useProject();

  return (
    <TaskTooltip taskNumber={data.number} project={project}>
      <Link
        style={{ textDecoration: 'none' }}
        className="reference"
        to={getTaskUrl(project.id, data.number)}
      >
        <span>{data.name} </span>
        <StyledId>#{data.number} </StyledId>
      </Link>
    </TaskTooltip>
  );
};
