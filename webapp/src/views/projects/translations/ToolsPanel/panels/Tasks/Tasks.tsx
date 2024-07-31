import React, { useEffect } from 'react';
import { T } from '@tolgee/react';
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
  const tasksLoadable = useApiQuery({
    url: '/v2/projects/{projectId}/tasks',
    method: 'get',
    path: { projectId: project.id },
    query: {
      filterTranslation: [translation.id],
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
          <TaskLabel key={task.id} task={task} sx={{ padding: 1 }} />
        ))
      ) : tasksLoadable.isLoading ? (
        <TabMessage>
          <LoadingSkeletonFadingIn variant="text" />
        </TabMessage>
      ) : (
        <TabMessage>
          <T keyName="translations_comments_no_comments"></T>
        </TabMessage>
      )}
    </StyledContainer>
  );
};

export const CommentsItemsCount = ({ keyData, language }: PanelContentData) => {
  const translation = keyData.translations[language.tag] as
    | TranslationViewModel
    | undefined;
  return <>{translation?.commentCount ?? 0}</>;
};
