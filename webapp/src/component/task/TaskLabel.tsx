import { Box, styled, SxProps, Tooltip } from '@mui/material';
import { FlagImage } from 'tg.component/languages/FlagImage';
import { components } from 'tg.service/apiSchema.generated';
import { TaskId } from './TaskId';
import { TaskTypeChip } from './TaskTypeChip';

type TaskModel = components['schemas']['TaskModel'];

const StyledContainer = styled(Box)`
  display: grid;
  grid-auto-flow: column;
  align-items: center;
  justify-content: start;
  gap: 8px;
`;

const StyledTaskName = styled(Box)`
  display: block;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
`;

type Props = {
  task: TaskModel;
  sx?: SxProps;
  className?: string;
};

export const TaskLabel = ({ task, sx, className }: Props) => {
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
      <StyledTaskName sx={{ flexShrink: 1 }}>{task.name}</StyledTaskName>
      <TaskId>{task.id}</TaskId>
      <TaskTypeChip type={task.type} />
    </StyledContainer>
  );
};
