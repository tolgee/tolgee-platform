import { useProject } from 'tg.hooks/useProject';
import { components, operations } from 'tg.service/apiSchema.generated';
import { TaskFilterType } from 'tg.ee/task/components/taskFilter/TaskFilterPopover';
import { TasksBoard } from 'tg.ee/task/components/TasksBoard';

import { useProjectBoardTasks } from './useProjectBoardTasks';

type TaskModel = components['schemas']['TaskModel'];
type QueryParameters = operations['getTasks_1']['parameters']['query'];

type Props = {
  showClosed: boolean;
  filter: TaskFilterType;
  onOpenDetail: (task: TaskModel) => void;
  search: string;
};

export const ProjectTasksBoard = ({
  showClosed,
  filter,
  onOpenDetail,
  search,
}: Props) => {
  const project = useProject();

  const query = {
    size: 10,
    search,
    sort: ['number,desc'],
    filterAssignee: filter.assignees,
    filterLanguage: filter.languages,
    filterType: filter.types,
  } satisfies QueryParameters;

  const newTasks = useProjectBoardTasks({
    projectId: project.id,
    query: { ...query, filterState: ['NEW'] },
  });

  const inProgressTasks = useProjectBoardTasks({
    projectId: project.id,
    query: { ...query, filterState: ['IN_PROGRESS'] },
  });

  const doneTasks = useProjectBoardTasks({
    projectId: project.id,
    query: {
      ...query,
      filterState: showClosed ? ['DONE', 'CLOSED'] : ['DONE'],
      filterDoneMinClosedAt: filter.doneMinClosedAt,
    },
  });

  return (
    <TasksBoard
      showClosed={showClosed}
      onOpenDetail={onOpenDetail}
      doneTasks={doneTasks}
      inProgressTasks={inProgressTasks}
      newTasks={newTasks}
      project={project}
      newTaskActions={true}
    />
  );
};
