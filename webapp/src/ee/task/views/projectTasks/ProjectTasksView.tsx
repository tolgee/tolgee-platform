import { useState } from 'react';
import { Box, Dialog, useMediaQuery } from '@mui/material';
import { useTranslate } from '@tolgee/react';

import { useProject } from 'tg.hooks/useProject';
import { LINKS, PARAMS } from 'tg.constants/links';
import { useApiQuery } from 'tg.service/http/useQueryApi';
import { components } from 'tg.service/apiSchema.generated';
import { useUrlSearchState } from 'tg.hooks/useUrlSearchState';
import { projectPreferencesService } from 'tg.service/ProjectPreferencesService';
import { useProjectPermissions } from 'tg.hooks/useProjectPermissions';
import { useGlobalContext } from 'tg.globalContext/GlobalContext';
import { TaskFilterType } from 'tg.ee/task/components/taskFilter/TaskFilterPopover';
import { BaseProjectView } from 'tg.views/projects/BaseProjectView';
import { TasksHeader } from 'tg.ee/task/components/tasksHeader/TasksHeader';
import { TaskView } from 'tg.ee/task/components/tasksHeader/TasksHeaderBig';
import { TaskCreateDialog } from 'tg.ee/task/components/taskCreate/TaskCreateDialog';
import { TaskDetail } from 'tg.ee/task/components/TaskDetail';

import { TasksList } from './TasksList';
import { ProjectTasksBoard } from './ProjectTasksBoard';
import { useEnabledFeatures } from 'tg.globalContext/helpers';
import { PaidFeatureBanner } from 'tg.ee/common/PaidFeatureBanner';

type TaskModel = components['schemas']['TaskModel'];

const DAY = 1000 * 60 * 60 * 24;

export const ProjectTasksView = () => {
  const project = useProject();
  const { t } = useTranslate();
  const [search, setSearch] = useUrlSearchState('search', { defaultVal: '' });
  const [showClosed, setShowClosed] = useUrlSearchState('showClosed', {
    defaultVal: 'false',
  });
  const { satisfiesPermission } = useProjectPermissions();
  const [view, setView] = useUrlSearchState('view', {
    defaultVal: 'LIST',
  });
  const [minus30Days] = useState(Date.now() - DAY * 30);

  const canEditTasks = satisfiesPermission('tasks.edit');

  const rightPanelWidth = useGlobalContext((c) => c.layout.rightPanelWidth);

  const isSmall = useMediaQuery(
    `@media(max-width: ${rightPanelWidth + 1000}px)`
  );

  const languagesLoadable = useApiQuery({
    url: '/v2/projects/{projectId}/languages',
    method: 'get',
    path: { projectId: project.id },
    query: { size: 10000 },
    options: {
      keepPreviousData: true,
    },
  });

  const [assignees, setAssignees] = useUrlSearchState('assignee', {
    array: true,
  });
  const [languages, setLanguages] = useUrlSearchState('language', {
    array: true,
  });
  const [types, setTypes] = useUrlSearchState('type', {
    array: true,
  });

  const filter: TaskFilterType = {
    assignees: assignees?.map((a) => Number(a)),
    languages: languages?.map((l) => Number(l)),
    types: types as any[],
    doneMinClosedAt: showClosed === 'true' ? undefined : minus30Days,
  };

  function setFilter(val: TaskFilterType) {
    setAssignees(val.assignees?.map((a) => String(a)));
    setLanguages(val.languages?.map((l) => String(l)));
    setTypes(val.types?.map((l) => String(l)));
  }

  const [detail, setDetail] = useState<TaskModel>();
  const [addDialog, setAddDialog] = useState(false);

  const allLanguages = languagesLoadable.data?._embedded?.languages ?? [];

  function handleDetailClose() {
    setDetail(undefined);
  }

  const languagesPreference = projectPreferencesService.getForProject(
    project.id
  );

  const { features } = useEnabledFeatures();
  const taskFeature = features.includes('TASKS');

  return (
    <BaseProjectView
      windowTitle={t('tasks_view_title')}
      title={t('tasks_view_title')}
      maxWidth={1200}
      stretch
      navigation={[
        [
          t('tasks_view_title'),
          LINKS.PROJECT_TASKS.build({
            [PARAMS.PROJECT_ID]: project.id,
          }),
        ],
      ]}
    >
      {!taskFeature ? (
        <Box>
          <PaidFeatureBanner customMessage={t('tasks_feature_description')} />
        </Box>
      ) : (
        <>
          <Box display="grid" gridTemplateRows="auto 1fr">
            <TasksHeader
              sx={{ mb: '20px', mt: '-12px' }}
              onSearchChange={setSearch}
              showClosed={showClosed === 'true'}
              onShowClosedChange={(val) => setShowClosed(String(val))}
              filter={filter}
              onFilterChange={setFilter}
              onAddTask={canEditTasks ? () => setAddDialog(true) : undefined}
              view={view as TaskView}
              onViewChange={setView}
              isSmall={isSmall}
              project={project}
            />

            {view === 'LIST' && !isSmall ? (
              <TasksList
                search={search}
                filter={filter}
                showClosed={showClosed === 'true'}
                onOpenDetail={setDetail}
                newTaskActions={true}
              />
            ) : (
              <ProjectTasksBoard
                search={search}
                filter={filter}
                showClosed={showClosed === 'true'}
                onOpenDetail={setDetail}
              />
            )}
            {detail !== undefined && (
              <Dialog open={true} onClose={handleDetailClose} maxWidth="xl">
                <TaskDetail
                  taskNumber={detail.number}
                  onClose={handleDetailClose}
                  projectId={project.id}
                  task={detail}
                />
              </Dialog>
            )}
            {addDialog && (
              <TaskCreateDialog
                open={addDialog}
                onClose={() => setAddDialog(false)}
                onFinished={() => setAddDialog(false)}
                initialValues={{
                  languages: allLanguages
                    .filter((l) => languagesPreference.includes(l.tag))
                    .filter((l) => !l.base)
                    .map((l) => l.id),
                }}
                projectId={project.id}
                allLanguages={allLanguages}
              />
            )}
          </Box>
        </>
      )}
    </BaseProjectView>
  );
};
