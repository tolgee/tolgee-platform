import { default as React, FunctionComponent } from 'react';
import { Box, styled } from '@mui/material';
import { T } from '@tolgee/react';
import {
  NotificationItem,
  NotificationItemProps,
} from 'tg.component/layout/Notifications/NotificationItem';
import { getTaskUrl } from 'tg.constants/links';

const StyledLinkedDetailItem = styled(Box)`
  margin-right: 10px;
  display: inline;
`;

const StyledLinkedDetailNumber = styled(StyledLinkedDetailItem)`
  color: ${({ theme }) => theme.palette.text.secondary};
`;

type TaskCompletedItemProps = NotificationItemProps;

export const TaskCompletedItem: FunctionComponent<TaskCompletedItemProps> = ({
  notification,
  ...props
}) => {
  const destinationUrl = getTaskUrl(
    notification.project!.id,
    notification.linkedTask!.number
  );
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
        <StyledLinkedDetailItem>
          {notification.linkedTask?.language.flagEmoji}
        </StyledLinkedDetailItem>
        <StyledLinkedDetailItem>
          {notification.linkedTask?.name}
        </StyledLinkedDetailItem>
        <StyledLinkedDetailNumber>
          #{notification.linkedTask?.number}
        </StyledLinkedDetailNumber>
      </Box>
    </NotificationItem>
  );
};
