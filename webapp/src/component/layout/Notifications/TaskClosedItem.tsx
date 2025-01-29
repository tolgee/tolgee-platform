import { default as React, FunctionComponent } from 'react';
import { T } from '@tolgee/react';
import {
  TaskItem,
  TaskItemProps,
} from 'tg.component/layout/Notifications/TaskItem';

type TaskClosedItemProps = TaskItemProps;

export const TaskClosedItem: FunctionComponent<TaskClosedItemProps> = ({
  notification,
  ...props
}) => {
  return (
    <TaskItem notification={notification} {...props}>
      <T keyName="notifications-task-closed" />
    </TaskItem>
  );
};
