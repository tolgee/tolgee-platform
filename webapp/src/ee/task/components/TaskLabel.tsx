import { Box, styled, SxProps, Tooltip } from '@mui/material';
import { FlagImage } from 'tg.component/languages/FlagImage';
import { components } from 'tg.service/apiSchema.generated';
import { TaskNumber, TaskNumberWithLink } from './TaskId';
import { TaskTypeChip } from './TaskTypeChip';

type TaskModel = components['schemas']['TaskModel'];
type SimpleProjectModel = components['schemas']['SimpleProjectModel'];

const StyledContainer = styled(Box)`
  display: grid;
  grid-auto-flow: column;
  align-items: center;
  justify-content: start;
  gap: 8px;
  font-size: 16px;
`;

const StyledTaskName = styled(Box)`
  display: block;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
  font-size: 16px;
`;

type Props = {
  task: TaskModel;
  project?: SimpleProjectModel;
  sx?: SxProps;
  className?: string;
  hideType?: boolean;
};

export const TaskLabel = ({
  task,
  sx,
  className,
  project,
  hideType,
}: Props) => {
  return (
    <StyledContainer {...{ sx, className }}>
      <Tooltip
        title={`${task.language.name} (${task.language.tag})`}
        disableInteractive
      >
        <Box display="flex">
          <FlagImage flagEmoji={task.language.flagEmoji!} height={20} />
        </Box>
      </Tooltip>
      <StyledTaskName data-cy="task-label-name" sx={{ flexShrink: 1 }}>
        {task.name}
      </StyledTaskName>
      {project ? (
        <TaskNumberWithLink project={project} taskNumber={task.number} />
      ) : (
        <TaskNumber taskNumber={task.number} />
      )}
      {!hideType && <TaskTypeChip type={task.type} />}
    </StyledContainer>
  );
};
