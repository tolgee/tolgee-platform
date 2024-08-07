import React, { useEffect } from 'react';
import { styled } from '@mui/material';
import { LoadingSkeletonFadingIn } from 'tg.component/LoadingSkeleton';

import {
  PanelContentData,
  PanelContentProps,
  TranslationViewModel,
} from '../../common/types';
import { TabMessage } from '../../common/TabMessage';
import { useApiQuery } from 'tg.service/http/useQueryApi';
import { TaskLabel } from 'tg.component/task/TaskLabel';

const StyledContainer = styled('div')`
  display: flex;
  flex-direction: column;
  align-items: stretch;
  margin-top: 4px;
`;

export const Tasks: React.FC<PanelContentProps> = ({
  keyData,
  language,
  setItemsCount,
  project,
}) => {
  const translation = keyData.translations[language.tag];
  const firstTask = translation.tasks?.[0];
  const tasksLoadable = useApiQuery({
    url: '/v2/projects/{projectId}/tasks',
    method: 'get',
    path: { projectId: project.id },
    query: {
      filterTranslation: [translation.id],
      filterState: ['IN_PROGRESS'],
      sort: ['type,desc', 'id,desc'],
    },
    options: {
      enabled: Boolean(translation),
    },
  });

  useEffect(() => {
    setItemsCount(tasksLoadable.data?._embedded?.tasks?.length);
  }, [tasksLoadable.data]);

  return (
    <StyledContainer>
      {tasksLoadable.data?._embedded?.tasks?.length ? (
        tasksLoadable.data._embedded.tasks.map((task) => (
          <TaskLabel
            key={task.id}
            task={task}
            sx={{ padding: 1, opacity: task.id === firstTask?.id ? 1 : 0.6 }}
          />
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
  const translation = keyData.translations[language.tag] as
    | TranslationViewModel
    | undefined;
  return translation?.tasks?.length ?? 0;
};
