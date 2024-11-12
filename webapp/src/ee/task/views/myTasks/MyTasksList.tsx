import { ListProps, PaperProps, styled } from '@mui/material';
import { PaginatedHateoasList } from 'tg.component/common/list/PaginatedHateoasList';
import { TaskFilterType } from 'tg.ee/task/components/taskFilter/TaskFilterPopover';
import { TaskItem } from 'tg.ee/task/components/TaskItem';
import { useUrlSearchState } from 'tg.hooks/useUrlSearchState';
import { components } from 'tg.service/apiSchema.generated';
import { useApiQuery } from 'tg.service/http/useQueryApi';

type TaskWithProjectModel = components['schemas']['TaskWithProjectModel'];

const StyledSeparator = styled('div')`
  grid-column: 1 / -1;
  height: 1px;
  background: ${({ theme }) => theme.palette.tokens.divider};
`;

type Props = {
  showClosed: boolean;
  filter: TaskFilterType;
  onOpenDetail: (task: TaskWithProjectModel) => void;
  search: string;
};

export const MyTasksList = ({
  showClosed,
  filter,
  search,
  onOpenDetail,
}: Props) => {
  const [page, setPage] = useUrlSearchState('page', { defaultVal: '0' });

  const tasksLoadable = useApiQuery({
    url: '/v2/user-tasks',
    method: 'get',
    query: {
      size: 20,
      page: Number(page),
      search,
      sort: ['number,desc'],
      filterNotState: showClosed ? undefined : ['CLOSED'],
      filterProject: filter.projects,
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
              '1fr minmax(15%, max-content) minmax(25%, max-content) minmax(5%, max-content) minmax(10%, max-content) auto',
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
          newTaskActions={false}
          task={task}
          onDetailOpen={(task) => onOpenDetail(task as TaskWithProjectModel)}
          project={task.project}
          showProject={true}
        />
      )}
      itemSeparator={() => <StyledSeparator />}
    />
  );
};
