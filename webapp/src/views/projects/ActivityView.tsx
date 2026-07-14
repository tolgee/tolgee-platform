import React from 'react';
import { Box, styled } from '@mui/material';
import { useTranslate } from '@tolgee/react';

import { BaseProjectView } from './BaseProjectView';
import { useApiQuery } from 'tg.service/http/useQueryApi';
import { useProject } from 'tg.hooks/useProject';
import { PaginatedHateoasList } from 'tg.component/common/list/PaginatedHateoasList';
import { ActivityGroupItem } from 'tg.component/activity/groups/ActivityGroupItem';
import { ProjectLanguagesProvider } from 'tg.hooks/ProjectLanguagesProvider';

const StyledList = styled(Box)`
  display: grid;
  gap: 8px;
  padding-top: 16px;
`;

export const ActivityView = () => {
  const { t } = useTranslate();
  const project = useProject();
  const [page, setPage] = React.useState(0);

  const groupsLoadable = useApiQuery({
    url: '/v2/projects/{projectId}/activity/groups',
    method: 'get',
    path: { projectId: project.id },
    query: { page, size: 20, sort: ['id,desc'] },
  });

  return (
    <BaseProjectView windowTitle={t('project_activity_title')} maxWidth={800}>
      <ProjectLanguagesProvider>
        <StyledList data-cy="activity-groups-list">
          <PaginatedHateoasList
            loadable={groupsLoadable}
            onPageChange={(p) => setPage(p)}
            renderItem={(item) => <ActivityGroupItem item={item} />}
          />
        </StyledList>
      </ProjectLanguagesProvider>
    </BaseProjectView>
  );
};
