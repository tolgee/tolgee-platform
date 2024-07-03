import React, { RefObject } from 'react';
import { Box, styled } from '@mui/material';
import { T } from '@tolgee/react';

import { useApiQuery } from 'tg.service/http/useQueryApi';
import { LoadingSkeleton } from 'tg.component/LoadingSkeleton';
import { useLoadingRegister } from 'tg.component/GlobalLoading';
import { stopAndPrevent } from 'tg.fixtures/eventHandler';
import { components } from 'tg.service/apiSchema.generated';
import { TaskLabel } from './TaskLabel';
import { TaskState } from './TaskState';
import { AlarmClock } from '@untitled-ui/icons-react';
import { useDateFormatter } from 'tg.hooks/useLocale';
import { BatchProgress } from 'tg.views/projects/translations/BatchOperations/OperationsSummary/BatchProgress';
import { useUserName } from 'tg.component/common/UserName';
import { TASK_ACTIVE_STATES } from './utils';

type TaskModel = components['schemas']['TaskModel'];

const StyledProgress = styled(Box)`
  display: flex;
  justify-content: space-between
  align-items: center;
  gap: 24px;
`;

type Props = {
  taskNumber: number;
  projectId: number;
  actions?: React.ReactNode | ((task: TaskModel) => React.ReactNode);
  popperRef: RefObject<any>;
};

export const TaskTooltipContent = ({
  projectId,
  taskNumber,
  actions,
  popperRef,
}: Props) => {
  const task = useApiQuery({
    url: '/v2/projects/{projectId}/tasks/{taskNumber}',
    method: 'get',
    path: {
      projectId,
      taskNumber,
    },
    fetchOptions: {
      disableAuthRedirect: true,
    },
    options: {
      onSettled() {
        popperRef.current?.update();
      },
    },
  });

  const formatDate = useDateFormatter();

  useLoadingRegister(task.isFetching);

  const assignees = task.data?.assignees ?? [];

  const getUserName = useUserName();

  return (
    <Box
      data-cy="task-tooltip-content"
      sx={{ padding: 1, maxWidth: 400, minWidth: 250, position: 'relative' }}
      onClick={stopAndPrevent()}
    >
      {task.isLoading && (
        <>
          <LoadingSkeleton sx={{ height: 24 }} />
          <LoadingSkeleton sx={{ height: 24 }} />
          <LoadingSkeleton sx={{ height: 24, width: '50%' }} />
        </>
      )}
      {task.error?.code === 'operation_not_permitted' && (
        <Box>
          <T keyName="task_tooltip_content_no_access" />
        </Box>
      )}
      {task.data && (
        <Box sx={{ display: 'grid', gap: 1, justifyContent: 'stretch' }}>
          <Box
            sx={{ display: 'flex', gap: 1, justifyContent: 'space-between' }}
          >
            <TaskLabel task={task.data} />
            {actions && (
              <Box
                sx={{
                  display: 'flex',
                  gap: 0.5,
                  margin: '-2px 0px',
                  alignItems: 'center',
                }}
                onClick={stopAndPrevent()}
              >
                {typeof actions === 'function' ? actions(task.data) : actions}
              </Box>
            )}
          </Box>
          <Box>
            {assignees.length ? (
              <>
                <T keyName="task_tooltip_content_assignees" />{' '}
                {getUserName(assignees[0]) +
                  (assignees.length > 1 ? ` (+${assignees.length - 1})` : '')}
              </>
            ) : (
              <T keyName="task_tooltip_content_no_assignees" />
            )}
          </Box>
          <StyledProgress>
            <Box display="flex" gap={1} alignItems="center">
              <TaskState state={task.data.state} />
              {TASK_ACTIVE_STATES.includes(task.data.state) && (
                <Box display="flex" width={100} alignItems="center">
                  <BatchProgress
                    progress={task.data.doneItems}
                    max={task.data.totalItems}
                  />
                </Box>
              )}
            </Box>
            {task.data.dueDate ? (
              <Box display="flex" alignItems="center" gap={0.5}>
                <AlarmClock style={{ width: 16, height: 16 }} />
                <Box>{formatDate(task.data.dueDate, { timeZone: 'UTC' })}</Box>
              </Box>
            ) : null}
          </StyledProgress>
        </Box>
      )}
    </Box>
  );
};
