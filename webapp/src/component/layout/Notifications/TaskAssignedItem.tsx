import { default as React, FunctionComponent } from 'react';
import { Box, styled } from '@mui/material';
import { T } from '@tolgee/react';
import {
  NotificationItem,
  NotificationItemProps,
} from 'tg.component/layout/Notifications/NotificationItem';
import { LINKS, PARAMS } from 'tg.constants/links';

const LinkedDetailItem = styled(Box)`
  margin-right: 10px;
  display: inline;
`;

const LinkedDetailNumber = styled(LinkedDetailItem)`
  color: ${({ theme }) => theme.palette.text.secondary};
`;

type TaskAssignedItemProps = NotificationItemProps;

export const TaskAssignedItem: FunctionComponent<TaskAssignedItemProps> = ({
  notification,
  ...props
}) => {
  const destinationUrl = `${LINKS.GO_TO_PROJECT_TASK.build({
    [PARAMS.PROJECT_ID]: notification.project!.id,
  })}?number=${notification.linkedTask!.number}`;
  return (
    <NotificationItem
      notification={notification}
      destinationUrl={destinationUrl}
      {...props}
    >
      <Box>
        <b>{notification.originatingUser?.name}</b>&nbsp;
        <T keyName="notifications-task-assigned" />
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
