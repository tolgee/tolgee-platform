import { default as React, FunctionComponent } from 'react';
import { Box, styled } from '@mui/material';
import { T } from '@tolgee/react';
import {
  NotificationItem,
  NotificationItemProps,
} from 'tg.component/layout/Notifications/NotificationItem';

const LinkedDetailItem = styled(Box)`
  margin-right: 10px;
  display: inline;
`;

const LinkedDetailNumber = styled(LinkedDetailItem)`
  color: ${({ theme }) =>
    theme.palette.mode === 'light'
      ? theme.palette.emphasis[400]
      : theme.palette.emphasis[600]};
`;

type TaskCompletedItemProps = NotificationItemProps;

export const TaskCompletedItem: FunctionComponent<TaskCompletedItemProps> = ({
  notification,
  ...props
}) => {
  const destinationUrl = `/projects/${notification.project?.id}/task?number=${notification.linkedTask?.number}`;
  return (
    <NotificationItem
      notification={notification}
      destinationUrl={destinationUrl}
      {...props}
    >
      <Box>
        <b>{notification.originatingUser?.name}</b>&nbsp;
        <T keyName="notifications-task-completed" />
      </Box>
      <Box>
        <LinkedDetailItem>
          {notification.linkedTask?.language.flagEmoji}
        </LinkedDetailItem>
        <LinkedDetailItem>{notification.linkedTask?.name}</LinkedDetailItem>
        <LinkedDetailNumber>
          #{notification.linkedTask?.number}
        </LinkedDetailNumber>
      </Box>
    </NotificationItem>
  );
};
