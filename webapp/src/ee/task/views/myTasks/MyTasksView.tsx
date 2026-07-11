import { useState } from 'react';
import { HomeLine } from '@untitled-ui/icons-react';
import { Dialog, useMediaQuery } from '@mui/material';
import { useTranslate } from '@tolgee/react';

import { BaseView } from 'tg.component/layout/BaseView';
import { DashboardPage } from 'tg.component/layout/DashboardPage';
import { LINKS, QUERY } from 'tg.constants/links';
import { components } from 'tg.service/apiSchema.generated';
import { useUrlSearchState } from 'tg.hooks/useUrlSearchState';
import { useGlobalContext } from 'tg.globalContext/GlobalContext';

import { MyTasksList } from './MyTasksList';
import { MyTasksBoard } from './MyTasksBoard';
import { TaskFilterType } from '../../components/taskFilter/TaskFilterPopover';
import { TasksHeader } from '../../components/tasksHeader/TasksHeader';
import { TaskView } from '../../components/tasksHeader/TasksHeaderBig';
import { TaskDetail } from '../../components/TaskDetail';

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
  const [showAll, setShowAll] = useUrlSearchState(
    QUERY.TASKS_FILTERS_SHOW_ALL,
    {
      defaultVal: 'false',
    }
  );

  const [projects, setProjects] = useUrlSearchState('project', {
    array: true,
  });
  const [types, setTypes] = useUrlSearchState('type', {
    array: true,
  });
  const [agencies, setAgencies] = useUrlSearchState('agency', {
    array: true,
  });

  const filter: TaskFilterType = {
    projects: projects?.map((p) => Number(p)),
    types: types as any[],
    filterNotClosedBefore: showAll === 'true' ? undefined : minus30Days,
    agencies: agencies?.map((a) => Number(a)),
  };

  const rightPanelWidth = useGlobalContext((c) => c.layout.rightPanelWidth);

  const isSmall = useMediaQuery(
    `@media(max-width: ${rightPanelWidth + 1000}px)`
  );

  function setFilter(val: TaskFilterType) {
    setProjects(val.projects?.map((p) => String(p)));
    setTypes(val.types?.map((l) => String(l)));
    setAgencies(val.agencies?.map((a) => String(a)));
  }

  function handleDetailClose() {
    setDetail(undefined);
  }

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
        <TasksHeader
          sx={{ mb: '20px', mt: '-12px' }}
          onSearchChange={setSearch}
          showAll={showAll === 'true'}
          onShowAllChange={(val) => setShowAll(String(val))}
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
            showAll={showAll === 'true'}
            onOpenDetail={setDetail}
          />
        ) : (
          <MyTasksBoard
            search={search}
            filter={filter}
            showAll={showAll === 'true'}
            onOpenDetail={setDetail}
          />
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
