import { useState } from 'react';
import { Dialog, ListProps, PaperProps, styled } from '@mui/material';
import { useTranslate } from '@tolgee/react';

import { useProject } from 'tg.hooks/useProject';
import { LINKS, PARAMS } from 'tg.constants/links';
import { PaginatedHateoasList } from 'tg.component/common/list/PaginatedHateoasList';
import { useApiQuery } from 'tg.service/http/useQueryApi';
import { TaskItem } from 'tg.component/task/TaskItem';
import { TaskDetail } from 'tg.component/task/TaskDetail';
import { components } from 'tg.service/apiSchema.generated';

import { BaseProjectView } from '../BaseProjectView';
import { TasksHeader } from './TasksHeader';
import { TaskFilterType } from 'tg.component/task/taskFilter/TaskFilterPopover';
import { useUrlSearchState } from 'tg.hooks/useUrlSearchState';

type TaskModel = components['schemas']['TaskModel'];

const StyledSeparator = styled('div')`
  grid-column: 1 / -1;
  height: 1px;
  background: ${({ theme }) => theme.palette.tokens.divider};
`;

export const ProjectTasksView = () => {
  const project = useProject();
  const { t } = useTranslate();
  const [page, setPage] = useState(0);
  const [search, setSearch] = useState('');
  const [showClosed, setShowClosed] = useState(false);
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
  };

  function setFilter(val: TaskFilterType) {
    setAssignees(val.assignees?.map((a) => String(a)));
    setLanguages(val.languages?.map((l) => String(l)));
    setTypes(val.types?.map((l) => String(l)));
  }

  const [detail, setDetail] = useState<TaskModel>();

  function handleDetailClose() {
    setDetail(undefined);
  }

  const tasksLoadable = useApiQuery({
    url: '/v2/projects/{projectId}/tasks',
    method: 'get',
    path: { projectId: project.id },
    query: {
      size: 20,
      page,
      search,
      sort: ['createdAt'],
      filterNotState: showClosed ? undefined : ['CLOSED', 'DONE'],
      filterAssignee: filter.assignees,
      filterLanguage: filter.languages,
      filterType: filter.types,
    },
    options: {
      keepPreviousData: true,
    },
  });

  return (
    <BaseProjectView
      windowTitle={t('tasks_view_title')}
      title={t('tasks_view_title')}
      maxWidth={900}
      navigation={[
        [
          t('tasks_view_title'),
          LINKS.PROJECT_TASKS.build({
            [PARAMS.PROJECT_ID]: project.id,
          }),
        ],
      ]}
    >
      <TasksHeader
        sx={{ mb: '20px', mt: '-12px' }}
        onSearchChange={setSearch}
        showClosed={showClosed}
        onShowClosedChange={setShowClosed}
        filter={filter}
        onFilterChange={setFilter}
      />
      <PaginatedHateoasList
        loadable={tasksLoadable}
        onPageChange={setPage}
        listComponentProps={
          {
            sx: {
              display: 'grid',
              gridTemplateColumns: '3fr 1fr 2fr 1fr auto',
              alignItems: 'center',
            },
          } as ListProps
        }
        wrapperComponentProps={
          {
            sx: {
              border: 'none',
              background: 'none',
            },
          } as PaperProps
        }
        renderItem={(task) => (
          <TaskItem
            task={task}
            onDetailOpen={(task) => setDetail(task)}
            project={project}
          />
        )}
        itemSeparator={() => <StyledSeparator />}
      />
      {detail !== undefined && (
        <Dialog open={true} onClose={handleDetailClose} maxWidth="xl">
          <TaskDetail
            task={detail}
            onClose={handleDetailClose}
            project={project}
          />
        </Dialog>
      )}
    </BaseProjectView>
  );
};
