import React from 'react';
import { T, useTranslate } from '@tolgee/react';
import { Box, IconButton, styled, Tooltip, useTheme } from '@mui/material';
import { AlertTriangle } from '@untitled-ui/icons-react';
import { Link } from 'react-router-dom';

import { useApiQuery } from 'tg.service/http/useQueryApi';
import { useProject } from 'tg.hooks/useProject';
import { TaskDetail as TaskDetailIcon } from 'tg.component/CustomIcons';
import { TaskLabel } from 'tg.ee/task/components/TaskLabel';
import { TaskTooltip } from 'tg.ee/task/components/TaskTooltip';
import {
  getTaskRedirect,
  TASK_ACTIVE_STATES,
} from 'tg.ee/task/components/utils';

import { PrefilterContainer } from './ContainerPrefilter';
import { useUrlSearchState } from 'tg.hooks/useUrlSearchState';
import { useUser } from 'tg.globalContext/helpers';
import { TaskState } from 'tg.ee/task/components/TaskState';

const StyledWarning = styled('div')`
  display: flex;
  align-items: center;
  padding-left: 12px;
  gap: 4px;
`;

const StyledTaskId = styled(Link)`
  text-decoration: underline;
  text-underline-offset: 3px;
  color: inherit;
`;

type Props = {
  taskNumber: number;
};

export const PrefilterTask = ({ taskNumber }: Props) => {
  const project = useProject();
  const theme = useTheme();
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

  if (!data) {
    return null;
  }

  function handleShowDetails() {
    setTaskDetail(String(taskNumber));
  }

  let alert: React.ReactNode | null = null;

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
              <TaskTooltip taskNumber={taskNumber} project={project}>
                <StyledTaskId to={getTaskRedirect(project, taskNumber)}>
                  #{taskNumber}
                </StyledTaskId>
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
        title={<T keyName="task_filter_indicator_label" />}
        content={
          <Box display="flex" gap={1} alignItems="center">
            <TaskLabel task={data} />
            <Tooltip title={t('task_detail_tooltip')} disableInteractive>
              <IconButton size="small" onClick={handleShowDetails}>
                <TaskDetailIcon width={20} height={20} />
              </IconButton>
            </Tooltip>
            {!isActive && <TaskState state={data.state} />}
            {alert ? (
              <StyledWarning>
                <AlertTriangle
                  width={18}
                  height={18}
                  color={theme.palette.warning.main}
                />
                <Box>{alert}</Box>
              </StyledWarning>
            ) : null}
          </Box>
        }
      />
    </>
  );
};
