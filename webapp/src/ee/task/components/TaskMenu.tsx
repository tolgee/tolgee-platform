import { useState } from 'react';
import { Dialog, Divider, Menu, MenuItem } from '@mui/material';
import { T, useTranslate } from '@tolgee/react';

import { confirmation } from 'tg.hooks/confirmation';
import { components } from 'tg.service/apiSchema.generated';
import { Scope } from 'tg.fixtures/permissions';
import { messageService } from 'tg.service/MessageService';
import { useApiMutation, useApiQuery } from 'tg.service/http/useQueryApi';

import { TASK_ACTIVE_STATES, useTaskReport } from './utils';
import { InitialValues, TaskCreateDialog } from './taskCreate/TaskCreateDialog';
import { useUser } from 'tg.globalContext/helpers';
import { TaskDetail } from './TaskDetail';

type TaskModel = components['schemas']['TaskModel'];
type SimpleProjectModel = components['schemas']['SimpleProjectModel'];

type Props = {
  anchorEl: HTMLElement | null;
  onClose: () => void;
  task: TaskModel;
  project: SimpleProjectModel;
  projectScopes?: Scope[];
  newTaskActions: boolean;
  hideTaskDetail?: boolean;
};

export const TaskMenu = ({
  anchorEl,
  onClose,
  task,
  project,
  projectScopes,
  newTaskActions,
  hideTaskDetail,
}: Props) => {
  const user = useUser();
  const isOpen = Boolean(anchorEl);
  const [taskCreate, setTaskCreate] = useState<Partial<InitialValues>>();
  const [taskDetail, setTaskDetail] = useState<TaskModel>();
  const closeMutation = useApiMutation({
    url: '/v2/projects/{projectId}/tasks/{taskNumber}/close',
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

  const { downloadReport } = useTaskReport();

  const projectLoadable = useApiQuery({
    url: '/v2/projects/{projectId}',
    method: 'get',
    path: { projectId: project.id },
    options: {
      enabled: !projectScopes && isOpen,
      refetchOnMount: false,
      staleTime: Infinity,
      cacheTime: Infinity,
    },
  });

  const scopes =
    projectScopes ?? projectLoadable.data?.computedPermission.scopes ?? [];

  const canEditTask = scopes?.includes('tasks.edit');
  const canMarkAsDone =
    scopes.includes('tasks.edit') ||
    Boolean(task.assignees.find((u) => u.id === user?.id));

  const languagesLoadable = useApiQuery({
    url: '/v2/projects/{projectId}/languages',
    method: 'get',
    path: { projectId: project.id },
    query: {
      page: 0,
      size: 1000,
      sort: ['tag'],
    },
    options: {
      enabled: Boolean(taskCreate),
    },
  });

  const taskKeysMutation = useApiMutation({
    url: '/v2/projects/{projectId}/tasks/{taskNumber}/keys',
    method: 'get',
  });

  function handleClose() {
    confirmation({
      title: <T keyName="task_menu_close_confirmation_title" />,
      onConfirm() {
        onClose();
        closeMutation.mutate(
          {
            path: { projectId: project.id, taskNumber: task.number },
          },
          {
            onSuccess() {
              messageService.success(<T keyName="task_menu_close_success" />);
            },
          }
        );
      },
    });
  }

  function handleReopen() {
    reopenMutation.mutate(
      {
        path: { projectId: project.id, taskNumber: task.number },
      },
      {
        onSuccess() {
          onClose();
          messageService.success(<T keyName="task_menu_reopen_success" />);
        },
      }
    );
  }

  function handleMarkAsDone() {
    finishMutation.mutate(
      {
        path: { projectId: project.id, taskNumber: task.number },
      },
      {
        onSuccess() {
          onClose();
          messageService.success(<T keyName="task_menu_finish_success" />);
        },
      }
    );
  }

  function handleGetExcelReport() {
    onClose();
    downloadReport(project.id, task);
  }

  function handleOpenDetail(task: TaskModel) {
    onClose();
    setTaskDetail(task);
  }

  function handleCloneTask() {
    taskKeysMutation.mutate(
      {
        path: { projectId: project.id, taskNumber: task.number },
      },
      {
        onSuccess(data) {
          setTaskCreate({
            selection: data.keys,
            name: task.name,
            description: task.description,
            type: task.type,
            dueDate: task.dueDate,
          });
          onClose();
        },
      }
    );
  }

  function handleCreateReviewTask() {
    taskKeysMutation.mutate(
      {
        path: { projectId: project.id, taskNumber: task.number },
      },
      {
        onSuccess(data) {
          setTaskCreate({
            selection: data.keys,
            name: task.name,
            description: task.description,
            languages: [task.language.id],
            type: 'REVIEW',
          });
          onClose();
        },
      }
    );
  }

  const { t } = useTranslate();
  return (
    <>
      <Menu anchorEl={anchorEl} open={isOpen} onClose={onClose}>
        {TASK_ACTIVE_STATES.includes(task.state) ? (
          <MenuItem
            onClick={handleMarkAsDone}
            disabled={task.doneItems !== task.totalItems || !canMarkAsDone}
          >
            {t('task_menu_mark_as_done')}
          </MenuItem>
        ) : (
          <MenuItem onClick={handleReopen} disabled={!canEditTask}>
            {t('task_menu_mark_as_in_progress')}
          </MenuItem>
        )}
        {TASK_ACTIVE_STATES.includes(task.state) && (
          <MenuItem disabled={!canEditTask} onClick={handleClose}>
            {t('task_menu_close_task')}
          </MenuItem>
        )}
        {!hideTaskDetail && (
          <MenuItem onClick={() => handleOpenDetail(task)}>
            {t('task_menu_detail')}
          </MenuItem>
        )}
        <Divider />
        {newTaskActions && (
          <MenuItem onClick={handleCloneTask} disabled={!canEditTask}>
            {t('task_menu_clone_task')}
          </MenuItem>
        )}

        {task.type === 'TRANSLATE' && newTaskActions && (
          <MenuItem onClick={handleCreateReviewTask} disabled={!canEditTask}>
            {t('task_menu_create_review_task')}
          </MenuItem>
        )}

        {newTaskActions && <Divider />}

        <MenuItem onClick={handleGetExcelReport}>
          {t('task_menu_generate_report')}
        </MenuItem>
      </Menu>
      {taskCreate && languagesLoadable.data && (
        <TaskCreateDialog
          open={true}
          onClose={() => setTaskCreate(undefined)}
          onFinished={() => setTaskCreate(undefined)}
          allLanguages={languagesLoadable.data._embedded?.languages ?? []}
          projectId={project.id}
          initialValues={taskCreate}
        />
      )}
      {taskDetail && (
        <Dialog
          open={true}
          onClose={() => setTaskDetail(undefined)}
          maxWidth="xl"
        >
          <TaskDetail
            taskNumber={taskDetail.number}
            projectId={project.id}
            onClose={() => setTaskDetail(undefined)}
            task={taskDetail}
          />
        </Dialog>
      )}
    </>
  );
};
