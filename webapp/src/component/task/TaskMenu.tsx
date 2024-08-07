import { Divider, Menu, MenuItem, useTheme } from '@mui/material';
import { T, useTranslate } from '@tolgee/react';
import { Link } from 'react-router-dom';

import { confirmation } from 'tg.hooks/confirmation';
import { components } from 'tg.service/apiSchema.generated';
import { useApiMutation } from 'tg.service/http/useQueryApi';
import { messageService } from 'tg.service/MessageService';
import { getLinkToTask } from './utils';

type TaskModel = components['schemas']['TaskModel'];
type SimpleProjectModel = components['schemas']['SimpleProjectModel'];

type Props = {
  anchorEl: HTMLElement | null;
  onClose: () => void;
  onDetailOpen: (task: TaskModel) => void;
  task: TaskModel;
  project: SimpleProjectModel;
};

export const TaskMenu = ({
  anchorEl,
  onClose,
  task,
  onDetailOpen,
  project,
}: Props) => {
  const updateMutation = useApiMutation({
    url: '/v2/projects/{projectId}/tasks/{taskId}',
    method: 'put',
    invalidatePrefix: ['/v2/projects/{projectId}/tasks', '/v2/user-tasks'],
  });

  function handleClose() {
    confirmation({
      title: <T keyName="task_menu_close_confirmation_title" />,
      onConfirm() {
        onClose();
        updateMutation.mutate(
          {
            path: { projectId: project.id, taskId: task.id },
            content: { 'application/json': { state: 'CLOSED' } },
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

  function handleChangeState(state: TaskModel['state']) {
    updateMutation.mutate(
      {
        path: { projectId: project.id, taskId: task.id },
        content: { 'application/json': { state } },
      },
      {
        onSuccess() {
          onClose();
          messageService.success(
            <T keyName="task_menu_state_changed_success" />
          );
        },
      }
    );
  }

  const withClose = (func: () => void) => () => {
    func();
    onClose();
  };

  const { t } = useTranslate();
  const theme = useTheme();
  return (
    <Menu anchorEl={anchorEl} open={Boolean(anchorEl)} onClose={onClose}>
      <MenuItem
        component={Link}
        to={getLinkToTask(project, task)}
        style={{
          textDecoration: 'none',
          color: theme.palette.text.primary,
          outline: 'none',
        }}
      >
        {t('task_menu_open_translations')}
      </MenuItem>
      <MenuItem onClick={withClose(() => onDetailOpen(task))}>
        {t('task_menu_task_detail')}
      </MenuItem>
      {task.state === 'IN_PROGRESS' ? (
        <MenuItem
          onClick={() => handleChangeState('DONE')}
          disabled={task.doneItems !== task.totalItems}
        >
          {t('task_menu_mark_as_done')}
        </MenuItem>
      ) : (
        <MenuItem onClick={() => handleChangeState('IN_PROGRESS')}>
          {t('task_menu_mark_as_in_progress')}
        </MenuItem>
      )}
      {task.state === 'IN_PROGRESS' && (
        <MenuItem onClick={handleClose}>{t('task_menu_close_task')}</MenuItem>
      )}
      <Divider />
      <MenuItem onClick={onClose}>{t('task_menu_clone_task')}</MenuItem>
      <MenuItem onClick={onClose}>{t('task_menu_create_review_task')}</MenuItem>
      <Divider />
      <MenuItem onClick={onClose}>{t('task_menu_generate_report')}</MenuItem>
    </Menu>
  );
};
