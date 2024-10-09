import { useState } from 'react';
import { HomeLine } from '@untitled-ui/icons-react';
import { Dialog, useMediaQuery } from '@mui/material';
import { useTranslate } from '@tolgee/react';

import { BaseView } from 'tg.component/layout/BaseView';
import { DashboardPage } from 'tg.component/layout/DashboardPage';
import { LINKS } from 'tg.constants/links';
import { components } from 'tg.service/apiSchema.generated';
import { useUrlSearchState } from 'tg.hooks/useUrlSearchState';
import { useGlobalContext } from 'tg.globalContext/GlobalContext';
import { TaskFilterType } from 'tg.ee/task/components/taskFilter/TaskFilterPopover';
import { TasksHeader } from 'tg.ee/task/components/tasksHeader/TasksHeader';
import { TaskView } from 'tg.ee/task/components/tasksHeader/TasksHeaderBig';
import { TaskDetail } from 'tg.ee/task/components/TaskDetail';

import { MyTasksList } from './MyTasksList';
import { MyTasksBoard } from './MyTasksBoard';
import { useEnabledFeatures } from 'tg.globalContext/helpers';
import { PaidFeatureBanner } from 'tg.ee/common/PaidFeatureBanner';

type TaskWithProjectModel = components['schemas']['TaskWithProjectModel'];

const DAY = 1000 * 60 * 60 * 24;

export const MyTasksView = () => {
  const { t } = useTranslate();
  const [detail, setDetail] = useState<TaskWithProjectModel>();
  const [minus30Days] = useState(Date.now() - DAY * 30);
  const [view, setView] = useUrlSearchState('view', {
    defaultVal: 'LIST',
  });

  const [search, setSearch] = useUrlSearchState('search', { defaultVal: '' });
  const [showClosed, setShowClosed] = useUrlSearchState('showClosed', {
    defaultVal: 'false',
  });

  const [projects, setProjects] = useUrlSearchState('project', {
    array: true,
  });
  const [types, setTypes] = useUrlSearchState('type', {
    array: true,
  });

  const filter: TaskFilterType = {
    projects: projects?.map((p) => Number(p)),
    types: types as any[],
    doneMinClosedAt: showClosed === 'true' ? undefined : minus30Days,
  };

  const rightPanelWidth = useGlobalContext((c) => c.layout.rightPanelWidth);

  const isSmall = useMediaQuery(
    `@media(max-width: ${rightPanelWidth + 1000}px)`
  );

  function setFilter(val: TaskFilterType) {
    setProjects(val.projects?.map((p) => String(p)));
    setTypes(val.types?.map((l) => String(l)));
  }

  function handleDetailClose() {
    setDetail(undefined);
  }

  const { features } = useEnabledFeatures();

  const taskFeature = features.includes('TASKS');

  return (
    <DashboardPage>
      <BaseView
        windowTitle={t('my_tasks_title')}
        title={t('my_tasks_title')}
        maxWidth={1200}
        navigation={[
          [
            null,
            LINKS.ROOT.build(),
            <HomeLine key={0} width={20} height={20} />,
          ],
          [t('my_tasks_title'), LINKS.MY_TASKS.build()],
        ]}
      >
        {!taskFeature ? (
          <PaidFeatureBanner customMessage={t('tasks_feature_description')} />
        ) : (
          <>
            <TasksHeader
              sx={{ mb: '20px', mt: '-12px' }}
              onSearchChange={setSearch}
              showClosed={showClosed === 'true'}
              onShowClosedChange={(val) => setShowClosed(String(val))}
              filter={filter}
              onFilterChange={setFilter}
              view={view as TaskView}
              onViewChange={setView}
              isSmall={isSmall}
            />
            {view === 'LIST' && !isSmall ? (
              <MyTasksList
                search={search}
                filter={filter}
                showClosed={showClosed === 'true'}
                onOpenDetail={setDetail}
              />
            ) : (
              <MyTasksBoard
                search={search}
                filter={filter}
                showClosed={showClosed === 'true'}
                onOpenDetail={setDetail}
              />
            )}
          </>
        )}
      </BaseView>
      {detail !== undefined && (
        <Dialog open={true} onClose={handleDetailClose} maxWidth="xl">
          <TaskDetail
            taskNumber={detail.number}
            onClose={handleDetailClose}
            projectId={detail.project.id}
            task={detail}
          />
        </Dialog>
      )}
    </DashboardPage>
  );
};
