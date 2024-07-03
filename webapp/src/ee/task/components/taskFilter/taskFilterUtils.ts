import { TaskFilterType } from './TaskFilterPopover';

export const filterEmpty = (filter: TaskFilterType) => {
  return (
    !filter.assignees?.length &&
    !filter.languages?.length &&
    !filter.projects?.length &&
    !filter.types?.length
  );
};
