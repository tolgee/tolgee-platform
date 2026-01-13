import { Box, styled, SxProps, Tooltip, Typography } from '@mui/material';
import { FlagImage } from '@tginternal/library/components/languages/FlagImage';
import { components } from 'tg.service/apiSchema.generated';
import { TaskNumber, TaskNumberWithLink } from './TaskId';
import { TaskTypeChip } from 'tg.component/task/TaskTypeChip';
import { AgencyLabel } from 'tg.ee';
import { T, useTranslate } from '@tolgee/react';
import clsx from 'clsx';
import { BranchNameChip } from 'tg.component/branching/BranchNameChip';

type TaskModel = components['schemas']['TaskModel'];
type SimpleProjectModel = components['schemas']['SimpleProjectModel'];
type TaskModelWithOrigin = TaskModel & { originBranchName?: string };

const StyledContainer = styled(Box)`
  display: grid;
  grid-auto-flow: column;
  align-items: center;
  justify-content: start;
  gap: 8px;
  font-size: 16px;

  &.canceled {
    opacity: 0.6;
    filter: grayscale(1);
  }

  &.finished {
    opacity: 0.6;
  }
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
  const { t } = useTranslate();
  const originBranchName = (task as TaskModelWithOrigin).originBranchName;
  return (
    <StyledContainer
      {...{
        sx,
      }}
      className={clsx(className, {
        canceled: task.state === 'CANCELED',
        finished: task.state === 'FINISHED',
      })}
    >
      <Tooltip
        title={`${task.language.name} (${task.language.tag})`}
        disableInteractive
      >
        <Box display="flex">
          <FlagImage flagEmoji={task.language.flagEmoji!} height={20} />
        </Box>
      </Tooltip>
      <StyledTaskName data-cy="task-label-name" sx={{ flexShrink: 1 }}>
        {task.name || t('task_default_name')}
      </StyledTaskName>
      {project ? (
        <TaskNumberWithLink project={project} task={task} />
      ) : (
        <TaskNumber taskNumber={task.number} />
      )}
      {!hideType && <TaskTypeChip type={task.type} />}
      {originBranchName && (
        <Typography
          color="text.secondary"
          fontSize={14}
          display="flex"
          alignItems="center"
          gap={1}
        >
          <T
            keyName="task_label_from_branch"
            params={{
              branch: <BranchNameChip name={originBranchName} size="small" />,
            }}
          />
        </Typography>
      )}
      {task.agency && (
        <Tooltip title={t('task_label_agency_tooltip')}>
          <span>
            <AgencyLabel agency={task.agency} />
          </span>
        </Tooltip>
      )}
    </StyledContainer>
  );
};
