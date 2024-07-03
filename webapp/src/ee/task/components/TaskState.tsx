import { styled, useTheme } from '@mui/material';
import { components } from 'tg.service/apiSchema.generated';
import { useTaskStateTranslation } from 'tg.translationTools/useTaskStateTranslation';

type TaskState = components['schemas']['TaskModel']['state'];

const StyledContainer = styled('span')`
  font-weight: 500;
`;

type Props = {
  state: TaskState;
};

export const useStateColor = () => {
  const theme = useTheme();

  return (state: TaskState) =>
    state === 'DONE'
      ? theme.palette.tokens._components.progressbar.task.done
      : state === 'IN_PROGRESS'
      ? theme.palette.tokens._components.progressbar.task.inProgress
      : theme.palette.tokens.text.secondary;
};

export const TaskState = ({ state }: Props) => {
  const translateState = useTaskStateTranslation();
  const stateColor = useStateColor();

  return (
    <StyledContainer style={{ color: stateColor(state) }} data-cy="task-state">
      {translateState(state)}
    </StyledContainer>
  );
};
