import { ListProps, PaperProps, styled } from '@mui/material';
import { PaginatedHateoasList } from 'tg.component/common/list/PaginatedHateoasList';
import { TaskFilterType } from 'tg.ee/task/components/taskFilter/TaskFilterPopover';
import { TaskItem } from 'tg.ee/task/components/TaskItem';
import { useProject } from 'tg.hooks/useProject';
import { useUrlSearchState } from 'tg.hooks/useUrlSearchState';
import { components } from 'tg.service/apiSchema.generated';
import { useApiQuery } from 'tg.service/http/useQueryApi';

type TaskModel = components['schemas']['TaskModel'];

const StyledSeparator = styled('div')`
  grid-column: 1 / -1;
  height: 1px;
  background: ${({ theme }) => theme.palette.tokens.divider};
`;

type Props = {
  showClosed: boolean;
  filter: TaskFilterType;
  onOpenDetail: (task: TaskModel) => void;
  search: string;
  newTaskActions: boolean;
};

export const TasksList = ({
  showClosed,
  filter,
  search,
  onOpenDetail,
  newTaskActions,
}: Props) => {
  const project = useProject();
  const [page, setPage] = useUrlSearchState('page', { defaultVal: '0' });

  const tasksLoadable = useApiQuery({
    url: '/v2/projects/{projectId}/tasks',
    method: 'get',
    path: { projectId: project.id },
    query: {
      size: 20,
      page: Number(page),
      search,
      sort: ['number,desc'],
      filterNotState: showClosed ? undefined : ['CLOSED'],
      filterAssignee: filter.assignees,
      filterLanguage: filter.languages,
      filterType: filter.types,
      filterDoneMinClosedAt: filter.doneMinClosedAt,
    },
    options: {
      keepPreviousData: true,
    },
  });

  return (
    <PaginatedHateoasList
      loadable={tasksLoadable}
      onPageChange={(val) => setPage(String(val))}
      listComponentProps={
        {
          sx: {
            display: 'grid',
            gridTemplateColumns:
              '1fr minmax(15%, max-content) minmax(25%, max-content) minmax(10%, max-content) auto',
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
          newTaskActions={newTaskActions}
          task={task}
          onDetailOpen={(task) => onOpenDetail(task)}
          project={project}
          projectScopes={project.computedPermission.scopes}
        />
      )}
      itemSeparator={() => <StyledSeparator />}
    />
  );
};
