import { Button } from '@mui/material';
import { T, useTranslate } from '@tolgee/react';

import { confirmation } from 'tg.hooks/confirmation';
import { components } from 'tg.service/apiSchema.generated';
import { Scope } from 'tg.fixtures/permissions';
import { messageService } from 'tg.service/MessageService';
import { useApiMutation } from 'tg.service/http/useQueryApi';

import { useUser } from 'tg.globalContext/helpers';
import { TASK_ACTIVE_STATES } from 'tg.component/task/taskActiveStates';

type TaskModel = components['schemas']['TaskModel'];

type Props = {
  task: TaskModel;
  projectId: number;
  projectScopes?: Scope[];
};

export const TaskDetailActions = ({
  task,
  projectId,
  projectScopes,
}: Props) => {
  const user = useUser();
  const cancelMutation = useApiMutation({
    url: '/v2/projects/{projectId}/tasks/{taskNumber}/cancel',
    method: 'put',
    invalidatePrefix: [
      '/v2/projects/{projectId}/translations',
      '/v2/projects/{projectId}/tasks',
      '/v2/user-tasks',
    ],
  });

  const reopenMutation = useApiMutation({
    url: '/v2/projects/{projectId}/tasks/{taskNumber}/reopen',
    method: 'put',
    invalidatePrefix: [
      '/v2/projects/{projectId}/translations',
      '/v2/projects/{projectId}/tasks',
      '/v2/user-tasks',
    ],
  });

  const finishMutation = useApiMutation({
    url: '/v2/projects/{projectId}/tasks/{taskNumber}/finish',
    method: 'put',
    invalidatePrefix: [
      '/v2/projects/{projectId}/translations',
      '/v2/projects/{projectId}/tasks',
      '/v2/user-tasks',
    ],
  });

  const canEditTask = projectScopes?.includes('tasks.edit');
  const canMarkAsDone =
    projectScopes?.includes('tasks.edit') ||
    Boolean(task.assignees.find((u) => u.id === user?.id));

  function handleClose() {
    confirmation({
      title: <T keyName="task_menu_cancel_confirmation_title" />,
      onConfirm() {
        cancelMutation.mutate(
          {
            path: { projectId, taskNumber: task.number },
          },
          {
            onSuccess() {
              messageService.success(<T keyName="task_menu_cancel_success" />);
            },
          }
        );
      },
    });
  }

  function handleReopen() {
    reopenMutation.mutate(
      {
        path: { projectId, taskNumber: task.number },
      },
      {
        onSuccess() {
          messageService.success(<T keyName="task_menu_reopen_success" />);
        },
      }
    );
  }

  function handleMarkAsDone() {
    finishMutation.mutate(
      {
        path: { projectId, taskNumber: task.number },
      },
      {
        onSuccess() {
          messageService.success(<T keyName="task_menu_finish_success" />);
        },
      }
    );
  }

  const { t } = useTranslate();
  return (
    <>
      {TASK_ACTIVE_STATES.includes(task.state) && (
        <Button
          variant="outlined"
          size="small"
          disabled={!canEditTask}
          onClick={handleClose}
        >
          {t('task_menu_cancel_task')}
        </Button>
      )}

      {TASK_ACTIVE_STATES.includes(task.state) ? (
        <Button
          variant="outlined"
          color="success"
          size="small"
          onClick={handleMarkAsDone}
          disabled={task.doneItems !== task.totalItems || !canMarkAsDone}
        >
          {t('task_menu_mark_as_finished')}
        </Button>
      ) : (
        <Button
          variant="outlined"
          size="small"
          onClick={handleReopen}
          disabled={!canEditTask}
        >
          {t('task_menu_mark_as_in_progress')}
        </Button>
      )}
    </>
  );
};
