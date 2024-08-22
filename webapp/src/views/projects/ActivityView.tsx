import { useTranslate } from '@tolgee/react';
import { BaseProjectView } from './BaseProjectView';
import { useApiQuery } from 'tg.service/http/useQueryApi';
import { useProject } from 'tg.hooks/useProject';
import { PaginatedHateoasList } from 'tg.component/common/list/PaginatedHateoasList';
import { ActivityGroupItem } from 'tg.component/activity/groups/ActivityGroupItem';
import { ProjectLanguagesProvider } from 'tg.hooks/ProjectLanguagesProvider';

export const ActivityView = () => {
  const { t } = useTranslate();

  const project = useProject();

  const groupsLoadable = useApiQuery({
    url: '/v2/projects/{projectId}/activity/groups',
    method: 'get',
    path: { projectId: project.id },
    query: {},
  });

  return (
    <BaseProjectView windowTitle={t('project-activity-title')}>
      <ProjectLanguagesProvider>
        <PaginatedHateoasList
          loadable={groupsLoadable}
          renderItem={(item) => <ActivityGroupItem item={item} />}
        />
      </ProjectLanguagesProvider>
    </BaseProjectView>
  );
};
