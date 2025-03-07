import { default as React, FunctionComponent } from 'react';
import { T } from '@tolgee/react';
import {
  TaskItem,
  TaskItemProps,
} from 'tg.component/layout/Notifications/TaskItem';

type Props = TaskItemProps;

export const TaskFinishedItem: FunctionComponent<Props> = ({
  notification,
  ...props
}) => {
  return (
    <TaskItem notification={notification} {...props}>
      <T keyName="notifications-task-finished" />
    </TaskItem>
  );
};
