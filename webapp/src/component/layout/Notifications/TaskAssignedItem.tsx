import { default as React, FunctionComponent } from 'react';
import { T } from '@tolgee/react';
import {
  TaskItem,
  TaskItemProps,
} from 'tg.component/layout/Notifications/TaskItem';

type TaskAssignedItemProps = TaskItemProps;

export const TaskAssignedItem: FunctionComponent<TaskAssignedItemProps> = ({
  notification,
  ...props
}) => {
  return (
    <TaskItem notification={notification} {...props}>
      <T keyName="notifications-task-assigned" />
    </TaskItem>
  );
};
