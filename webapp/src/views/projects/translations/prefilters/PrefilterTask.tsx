import React from 'react';
import { T, useTranslate } from '@tolgee/react';
import { Box, IconButton, styled, Tooltip } from '@mui/material';
import { AlertCircle, ClipboardCheck, X } from '@untitled-ui/icons-react';

import { useApiQuery } from 'tg.service/http/useQueryApi';
import { useProject } from 'tg.hooks/useProject';
import { TaskDetail as TaskDetailIcon } from 'tg.component/CustomIcons';
import { TaskLabel } from 'tg.ee/task/components/TaskLabel';
import { TaskTooltip } from 'tg.ee/task/components/TaskTooltip';
import { TASK_ACTIVE_STATES } from 'tg.ee/task/components/utils';

import { PrefilterContainer } from './ContainerPrefilter';
import { useUrlSearchState } from 'tg.hooks/useUrlSearchState';
import { useUser } from 'tg.globalContext/helpers';
import { TaskState } from 'tg.ee/task/components/TaskState';
import { usePrefilter } from './usePrefilter';

const StyledWarning = styled('div')`
  display: flex;
  align-items: center;
  padding-left: 12px;
  gap: 4px;
  color: ${({ theme }) => theme.palette.error.main};
  font-weight: 500;
`;

const StyledTaskId = styled('span')`
  text-decoration: underline;
  text-underline-offset: 3px;
  color: inherit;
  cursor: default;
`;

type Props = {
  taskNumber: number;
};

export const PrefilterTask = ({ taskNumber }: Props) => {
  const project = useProject();
  const { t } = useTranslate();
  const currentUser = useUser();

  const { data } = useApiQuery({
    url: '/v2/projects/{projectId}/tasks/{taskNumber}',
    method: 'get',
    path: { projectId: project.id, taskNumber },
  });

  const blockingTasksLoadable = useApiQuery({
    url: '/v2/projects/{projectId}/tasks/{taskNumber}/blocking-tasks',
    method: 'get',
    path: { projectId: project.id, taskNumber },
  });

  const [_, setTaskDetail] = useUrlSearchState('taskDetail');
  const { clear } = usePrefilter();

  function handleShowDetails() {
    setTaskDetail(String(taskNumber));
  }

  let alert: React.ReactNode | null = null;

  if (!data) {
    return null;
  }

  const isActive = TASK_ACTIVE_STATES.includes(data.state);

  if (isActive) {
    if (!data.assignees.find((u) => u.id === currentUser?.id)) {
      alert = <T keyName="task_filter_indicator_user_not_assigned" />;
    } else if (blockingTasksLoadable.data?.length) {
      alert = (
        <>
          <T keyName="task_filter_indicator_blocking_warning" />{' '}
          {blockingTasksLoadable.data.map((taskNumber, i) => (
            <React.Fragment key={taskNumber}>
              <TaskTooltip
                taskNumber={taskNumber}
                project={project}
                newTaskActions={true}
              >
                <StyledTaskId>#{taskNumber}</StyledTaskId>
              </TaskTooltip>
              {i !== blockingTasksLoadable.data.length - 1 && ', '}
            </React.Fragment>
          ))}
        </>
      );
    }
  }

  return (
    <>
      <PrefilterContainer
        icon={<ClipboardCheck />}
        title={<T keyName="task_filter_indicator_label" />}
        closeButton={
          <Tooltip title={t('task_filter_close_tooltip')} disableInteractive>
            <IconButton size="small" onClick={clear}>
              <X />
            </IconButton>
          </Tooltip>
        }
        content={
          <Box
            display="flex"
            gap={1}
            alignItems="center"
            whiteSpace="nowrap"
            pr={2}
          >
            <TaskLabel task={data} />
            <Tooltip title={t('task_detail_tooltip')} disableInteractive>
              <IconButton size="small" onClick={handleShowDetails}>
                <TaskDetailIcon width={20} height={20} />
              </IconButton>
            </Tooltip>
            {!isActive && <TaskState state={data.state} />}
            {alert ? (
              <StyledWarning>
                <AlertCircle width={20} height={20} />
                <Box>{alert}</Box>
              </StyledWarning>
            ) : null}
          </Box>
        }
      />
    </>
  );
};
