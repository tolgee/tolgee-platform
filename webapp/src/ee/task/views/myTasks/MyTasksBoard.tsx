import { components, operations } from 'tg.service/apiSchema.generated';
import { TaskFilterType } from 'tg.ee/task/components/taskFilter/TaskFilterPopover';
import { TasksBoard } from 'tg.ee/task/components/TasksBoard';

import { useMyBoardTask } from './useMyBoardTask';

type TaskWithProjectModel = components['schemas']['TaskWithProjectModel'];
type QueryParameters = operations['getTasks_1']['parameters']['query'];

type Props = {
  showClosed: boolean;
  filter: TaskFilterType;
  onOpenDetail: (task: TaskWithProjectModel) => void;
  search: string;
};

export const MyTasksBoard = ({
  showClosed,
  filter,
  onOpenDetail,
  search,
}: Props) => {
  const query = {
    size: 10,
    search,
    sort: ['number,desc'],
    filterProject: filter.projects,
    filterType: filter.types,
  } satisfies QueryParameters;

  const newTasks = useMyBoardTask({
    query: { ...query, filterState: ['NEW'] },
  });

  const inProgressTasks = useMyBoardTask({
    query: { ...query, filterState: ['IN_PROGRESS'] },
  });

  const doneTasks = useMyBoardTask({
    query: {
      ...query,
      filterState: showClosed ? ['DONE', 'CLOSED'] : ['DONE'],
      filterDoneMinClosedAt: filter.doneMinClosedAt,
    },
  });

  return (
    <TasksBoard
      showClosed={showClosed}
      onOpenDetail={(t) => onOpenDetail(t as TaskWithProjectModel)}
      doneTasks={doneTasks}
      inProgressTasks={inProgressTasks}
      newTasks={newTasks}
      newTaskActions={false}
    />
  );
};
