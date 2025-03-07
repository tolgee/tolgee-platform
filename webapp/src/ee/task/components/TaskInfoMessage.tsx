import { Alert, AlertColor, styled } from '@mui/material';
import { T } from '@tolgee/react';
import { components } from 'tg.service/apiSchema.generated';
import { useProject } from 'tg.hooks/useProject';
import { TaskTooltip } from './TaskTooltip';

type KeyTaskViewModel = components['schemas']['KeyTaskViewModel'];
type TaskModel = components['schemas']['TaskModel'];

const StyledTaskId = styled('span')`
  color: ${({ theme }) => theme.palette.primary.main};
  cursor: default;
  text-decoration-line: underline;
  text-decoration-style: solid;
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
  tasks: KeyTaskViewModel[] | undefined;
  currentTask: TaskModel | undefined;
};

function getMessage(
  tasks: KeyTaskViewModel[] | undefined,
  currentTask: TaskModel | undefined
):
  | {
      severity: AlertColor;
      content: React.ReactNode;
    }
  | undefined {
  const firstTask = tasks?.[0];
  const userAssignedTask = tasks?.find((t) => t.userAssigned);

  if (currentTask?.state === 'FINISHED') {
    return {
      severity: 'error',
      content: (
        <T
          keyName="task_info_message_current_task_finished"
          params={{
            currentTask: <TaskLink task={currentTask.number} />,
          }}
        />
      ),
    };
  } else if (currentTask?.state === 'CANCELED') {
    return {
      severity: 'error',
      content: (
        <T
          keyName="task_info_message_current_task_canceled"
          params={{
            currentTask: <TaskLink task={currentTask.number} />,
          }}
        />
      ),
    };
  }

  if (currentTask && firstTask && firstTask.number !== currentTask.number) {
    return {
      severity: 'error',
      content: (
        <T
          keyName="task_info_message_current_task_blocked"
          params={{
            currentTask: <TaskLink task={currentTask.number} />,
            blockingTask: <TaskLink task={firstTask.number} />,
          }}
        />
      ),
    };
  }

  if (currentTask && firstTask && firstTask.number !== currentTask.number) {
    return {
      severity: 'error',
      content: (
        <T
          keyName="task_info_message_current_task_blocked"
          params={{
            currentTask: <TaskLink task={currentTask.number} />,
            blockingTask: <TaskLink task={firstTask.number} />,
          }}
        />
      ),
    };
  }

  if (
    firstTask &&
    !firstTask?.userAssigned &&
    userAssignedTask &&
    currentTask?.number !== firstTask.number
  ) {
    return {
      severity: 'error',
      content: (
        <T
          keyName="task_info_message_assigned_to_blocked_task"
          params={{
            assignedTask: <TaskLink task={userAssignedTask.number} />,
            blockingTask: <TaskLink task={firstTask.number} />,
          }}
        />
      ),
    };
  }

  if (firstTask && firstTask.userAssigned && !currentTask) {
    if (firstTask.type === 'TRANSLATE') {
      return {
        severity: 'info',
        content: (
          <T
            keyName="task_info_message_in_translation_task"
            params={{ task: <TaskLink task={firstTask.number} /> }}
          />
        ),
      };
    } else {
      return {
        severity: 'info',
        content: (
          <T
            keyName="task_info_message_in_review_task"
            params={{ task: <TaskLink task={firstTask.number} /> }}
          />
        ),
      };
    }
  }

  if (firstTask && !firstTask?.userAssigned) {
    if (firstTask.type === 'TRANSLATE') {
      return {
        severity: 'error',
        content: (
          <T
            keyName="task_info_message_in_translation_task_unassigned"
            params={{ task: <TaskLink task={firstTask.number} /> }}
          />
        ),
      };
    } else {
      return {
        severity: 'error',
        content: (
          <T
            keyName="task_info_message_in_review_task_unassigned"
            params={{ task: <TaskLink task={firstTask.number} /> }}
          />
        ),
      };
    }
  }
}

export const TaskInfoMessage = ({ tasks, currentTask }: Props) => {
  const message = getMessage(tasks, currentTask);

  if (message) {
    return (
      <Alert
        severity={message.severity}
        icon={false}
        data-cy="task-info-message"
      >
        {message.content}
      </Alert>
    );
  }

  return null;
};
