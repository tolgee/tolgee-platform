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

export const TaskState = ({ state }: Props) => {
  const theme = useTheme();
  const translateState = useTaskStateTranslation();

  const color =
    state === 'DONE'
      ? theme.palette.tokens._components.progressbar.task.done
      : state === 'IN_PROGRESS'
      ? theme.palette.tokens._components.progressbar.task.inProgress
      : theme.palette.tokens.text.secondary;

  return (
    <StyledContainer style={{ color }}>{translateState(state)}</StyledContainer>
  );
};
