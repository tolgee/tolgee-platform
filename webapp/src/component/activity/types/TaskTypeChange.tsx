import { styled } from '@mui/material';
import { components } from 'tg.service/apiSchema.generated';
import { TaskTypeChip } from 'tg.ee/task/components/TaskTypeChip';

import { DiffValue } from '../types';

type Type = components['schemas']['TaskModel']['type'];

const StyledDiff = styled('span')`
  word-break: break-word;
`;

const StyledRemoved = styled('span')`
  text-decoration: line-through;
`;

const StyledArrow = styled('span')`
  padding: 0px 6px;
`;

type Props = {
  input?: DiffValue<Type>;
};

export const TaskTypeChange = ({ input }: Props) => {
  if (input?.new && input?.old) {
    return (
      <StyledDiff>
        <StyledRemoved>
          <TaskTypeChip type={input.old} />
        </StyledRemoved>
        <StyledArrow>→</StyledArrow>
        <span>
          <TaskTypeChip type={input.new} />
        </span>
      </StyledDiff>
    );
  }
  if (input?.new) {
    return (
      <span>
        <TaskTypeChip type={input.new} />
      </span>
    );
  } else if (input?.old) {
    return (
      <StyledRemoved>
        <TaskTypeChip type={input.old} />
      </StyledRemoved>
    );
  } else {
    return null;
  }
};
