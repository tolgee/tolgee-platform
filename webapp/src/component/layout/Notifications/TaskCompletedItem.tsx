import { default as React, FunctionComponent } from 'react';
import { T } from '@tolgee/react';
import {
  TaskItem,
  TaskItemProps,
} from 'tg.component/layout/Notifications/TaskItem';

type TaskCompletedItemProps = TaskItemProps;

export const TaskCompletedItem: FunctionComponent<TaskCompletedItemProps> = ({
  notification,
  ...props
}) => {
  return (
    <TaskItem notification={notification} {...props}>
      <T keyName="notifications-task-completed" />
    </TaskItem>
  );
};
