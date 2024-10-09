import { Box, Chip, styled } from '@mui/material';
import { components } from 'tg.service/apiSchema.generated';
import { useTaskStateTranslation } from 'tg.translationTools/useTaskStateTranslation';
import { Scope } from 'tg.fixtures/permissions';

import { BoardItem } from './BoardItem';
import { useStateColor } from './TaskState';

type TaskModel = components['schemas']['TaskModel'];
type TaskWithProjectModel = components['schemas']['TaskWithProjectModel'];
type SimpleProjectModel = components['schemas']['SimpleProjectModel'];

const StyledColumn = styled(Box)`
  display: grid;
  gap: 12px;
  align-content: start;
  flex-grow: 1;
  flex-basis: 200px;
`;

const StyledColumnTitle = styled(Box)`
  display: flex;
  justify-content: center;
  align-items: center;
  gap: 8px;
  text-transform: uppercase;
  font-size: 15px;
  font-weight: 500;
`;

const StyledEmptyMessage = styled(Box)`
  display: grid;
  height: 138px;
  padding: 20px;
  align-content: center;
  justify-content: center;
  font-style: italic;
  background: ${({ theme }) => theme.palette.tokens.background.hover};
`;

type Props = {
  state?: TaskModel['state'];
  tasks: (TaskModel | TaskWithProjectModel)[];
  total: number;
  project?: SimpleProjectModel;
  projectScopes?: Scope[];
  onDetailOpen: (task: TaskModel) => void;
  title?: React.ReactNode;
  emptyMessage: React.ReactNode;
  newTaskActions: boolean;
};

export const BoardColumn = ({
  state,
  tasks,
  total,
  project,
  projectScopes,
  onDetailOpen,
  title,
  emptyMessage,
  newTaskActions,
}: Props) => {
  const translateState = useTaskStateTranslation();
  const stateColor = useStateColor();

  return (
    <StyledColumn>
      <StyledColumnTitle>
        {title ??
          (state && (
            <Box color={stateColor(state)}>{translateState(state)}</Box>
          ))}
        <Chip label={total} size="small" />
      </StyledColumnTitle>

      {tasks.length ? (
        tasks.map((t) => (
          <BoardItem
            key={t.number}
            task={t}
            project={project ?? (t as TaskWithProjectModel).project}
            projectScopes={projectScopes}
            onDetailOpen={onDetailOpen}
            newTaskActions={newTaskActions}
          />
        ))
      ) : (
        <StyledEmptyMessage>{emptyMessage}</StyledEmptyMessage>
      )}
    </StyledColumn>
  );
};
