import React, { useEffect } from 'react';
import { Box, styled } from '@mui/material';
import { LoadingSkeletonFadingIn } from 'tg.component/LoadingSkeleton';
import { useApiQuery } from 'tg.service/http/useQueryApi';

import {
  PanelContentData,
  PanelContentProps,
} from 'tg.views/projects/translations/ToolsPanel/common/types';
import { TabMessage } from 'tg.views/projects/translations/ToolsPanel/common/TabMessage';
import { TaskTooltip } from './TaskTooltip';
import { TaskLabel } from './TaskLabel';
import { TASK_ACTIVE_STATES } from 'tg.component/task/taskActiveStates';

const StyledContainer = styled('div')`
  display: flex;
  flex-direction: column;
  align-items: stretch;
  margin-top: 4px;
`;

export const TasksPanel: React.FC<PanelContentProps> = ({
  keyData,
  language,
  setItemsCount,
  project,
}) => {
  const firstTask = keyData.tasks?.find((t) => t.languageId === language.id);
  const tasksLoadable = useApiQuery({
    url: '/v2/projects/{projectId}/tasks',
    method: 'get',
    path: { projectId: project.id },
    query: {
      filterKey: [keyData.keyId],
      filterLanguage: [language.id],
      filterState: TASK_ACTIVE_STATES,
      sort: ['type,desc', 'id,desc'],
    },
  });

  useEffect(() => {
    setItemsCount(tasksLoadable.data?._embedded?.tasks?.length);
  }, [tasksLoadable.data]);

  return (
    <StyledContainer>
      {tasksLoadable.data?._embedded?.tasks?.length ? (
        tasksLoadable.data._embedded.tasks.map((task) => (
          <TaskTooltip
            key={task.number}
            taskNumber={task.number}
            project={project}
            enterDelay={1000}
          >
            <Box>
              <TaskLabel
                task={task}
                sx={{
                  padding: 1,
                  opacity: task.number === firstTask?.number ? 1 : 0.6,
                }}
              />
            </Box>
          </TaskTooltip>
        ))
      ) : tasksLoadable.isLoading ? (
        <TabMessage>
          <LoadingSkeletonFadingIn variant="text" />
        </TabMessage>
      ) : null}
    </StyledContainer>
  );
};

export const tasksCount = ({ keyData, language }: PanelContentData) => {
  return (
    keyData.tasks?.filter((t) => t.languageId === language.id)?.length ?? 0
  );
};
