import { styled } from '@mui/material';
import { DiffValue } from '../types';
import { components } from 'tg.service/apiSchema.generated';
import { TaskState } from 'tg.ee/task/components/TaskState';

type TaskState = components['schemas']['TaskModel']['state'];

const StyledDiff = styled('span')`
  word-break: break-word;
`;

const StyledRemoved = styled('span')`
  text-decoration: line-through;
`;

const StyledArrow = styled('span')`
  padding: 0px 6px;
`;

export const getTaskStateChange = (input?: DiffValue<TaskState>) => {
  if (input?.new && input?.old) {
    return (
      <StyledDiff>
        <StyledRemoved>
          <TaskState state={input.old} />
        </StyledRemoved>
        <StyledArrow>→</StyledArrow>
        <span>
          <TaskState state={input.new} />
        </span>
      </StyledDiff>
    );
  }
  if (input?.new) {
    return (
      <span>
        <TaskState state={input.new} />
      </span>
    );
  } else if (input?.old) {
    return (
      <StyledRemoved>
        <TaskState state={input.old} />
      </StyledRemoved>
    );
  } else {
    return null;
  }
};
