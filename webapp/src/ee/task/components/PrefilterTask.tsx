import React from 'react';
import { T, useTranslate } from '@tolgee/react';
import { Box, IconButton, styled, Tooltip } from '@mui/material';
import {
  AlertCircle,
  ClipboardCheck,
  InfoCircle,
  X,
} from '@untitled-ui/icons-react';

import { useApiQuery } from 'tg.service/http/useQueryApi';
import { useProject } from 'tg.hooks/useProject';

import { PrefilterContainer } from 'tg.views/projects/translations/prefilters/ContainerPrefilter';
import { useUrlSearchState } from 'tg.hooks/useUrlSearchState';
import { useUser } from 'tg.globalContext/helpers';
import { usePrefilter } from 'tg.views/projects/translations/prefilters/usePrefilter';
import { TaskTooltip } from './TaskTooltip';
import { TaskLabel } from './TaskLabel';
import { PrefilterTaskProps } from '../../../eeSetup/EeModuleType';
import { TASK_ACTIVE_STATES } from 'tg.component/task/taskActiveStates';
import { QUERY } from 'tg.constants/links';
import { PrefilterTaskHideDoneSwitch } from './PrefilterTaskHideDoneSwitch';

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

export const PrefilterTask = ({ taskNumber }: PrefilterTaskProps) => {
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

  const [_, setTaskDetail] = useUrlSearchState(QUERY.TRANSLATIONS_TASK_DETAIL);

  const prefilter = usePrefilter();

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
              <TaskTooltip taskNumber={taskNumber} project={project}>
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
        title={
          data && data.name ? <T keyName="task_filter_indicator_label" /> : ''
        }
        closeButton={
          <Tooltip title={t('task_filter_close_tooltip')} disableInteractive>
            <IconButton size="small" onClick={prefilter?.clear}>
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
                <InfoCircle width={20} height={20} />
              </IconButton>
            </Tooltip>
          </Box>
        }
        controls={<PrefilterTaskHideDoneSwitch />}
        alert={
          Boolean(alert) && (
            <StyledWarning>
              <AlertCircle width={20} height={20} />
              <Box>{alert}</Box>
            </StyledWarning>
          )
        }
      />
    </>
  );
};
