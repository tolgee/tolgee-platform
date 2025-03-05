import { Alert, AlertTitle, styled } from '@mui/material';
import { T } from '@tolgee/react';
import { components } from 'tg.service/apiSchema.generated';
import { useProject } from 'tg.hooks/useProject';
import { TaskTooltip } from './TaskTooltip';

type TaskModel = components['schemas']['KeyTaskViewModel'];

const StyledTaskId = styled('span')`
  color: ${({ theme }) => theme.palette.primary.main};
  cursor: default;
  text-decoration-line: underline;
  text-decoration-style: solid;
  text-decoration-skip-ink: none;
  text-decoration-thickness: auto;
  text-underline-offset: auto;
  text-underline-position: from-font;
`;

const TaskLink = (props: { task: number }) => {
  const project = useProject();
  return (
    <TaskTooltip taskNumber={props.task} project={project}>
      <StyledTaskId>#{props.task}</StyledTaskId>
    </TaskTooltip>
  );
};

type Props = {
  tasks: TaskModel[] | undefined;
  currentTask: number | undefined;
};

export const TaskInfoMessage = ({ tasks, currentTask }: Props) => {
  const firstTask = tasks?.[0];
  const userAssignedTask = tasks?.find((t) => t.userAssigned);

  if (firstTask && currentTask && firstTask.number !== currentTask) {
    return (
      <Alert severity="error" icon={false}>
        <T
          keyName="task_info_message_current_task_blocked"
          params={{
            currentTask: <TaskLink task={currentTask} />,
            blockingTask: <TaskLink task={firstTask.number} />,
          }}
        />
      </Alert>
    );
  }

  if (
    firstTask &&
    !firstTask?.userAssigned &&
    userAssignedTask &&
    currentTask !== firstTask.number
  ) {
    return (
      <Alert severity="error" icon={false}>
        <T
          keyName="task_info_message_assigned_to_blocked_task"
          params={{
            assignedTask: <TaskLink task={userAssignedTask.number} />,
            blockingTask: <TaskLink task={firstTask.number} />,
          }}
        />
      </Alert>
    );
  }

  if (firstTask && firstTask.userAssigned && !currentTask) {
    if (firstTask.type === 'TRANSLATE') {
      return (
        <Alert severity="info" icon={false}>
          <T
            keyName="task_info_message_in_translation_task"
            params={{ task: <TaskLink task={firstTask.number} /> }}
          />
        </Alert>
      );
    } else {
      return (
        <Alert severity="info" icon={false}>
          <T
            keyName="task_info_message_in_review_task"
            params={{ task: <TaskLink task={firstTask.number} /> }}
          />
        </Alert>
      );
    }
  }

  if (firstTask && !firstTask?.userAssigned) {
    if (firstTask.type === 'TRANSLATE') {
      return (
        <Alert severity="error" icon={false}>
          <T
            keyName="task_info_message_in_translation_task_unassigned"
            params={{ task: <TaskLink task={firstTask.number} /> }}
          />
        </Alert>
      );
    } else {
      return (
        <Alert severity="error" icon={false}>
          <T
            keyName="task_info_message_in_review_task_unassigned"
            params={{ task: <TaskLink task={firstTask.number} /> }}
          />
        </Alert>
      );
    }
  }

  return null;
};
