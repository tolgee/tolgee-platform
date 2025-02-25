import { default as React, FunctionComponent } from 'react';
import { Box, styled } from '@mui/material';
import {
  NotificationItem,
  NotificationItemProps,
} from 'tg.component/layout/Notifications/NotificationItem';
import { getTaskUrl } from 'tg.constants/links';
import { FlagImage } from 'tg.component/languages/FlagImage';

const StyledLinkedDetailItem = styled(Box)`
  margin-right: 10px;
  display: inline;
`;

const StyledLinkedDetailNumber = styled(StyledLinkedDetailItem)`
  color: ${({ theme }) => theme.palette.text.secondary};
`;

export type TaskItemProps = NotificationItemProps;

export const TaskItem: FunctionComponent<TaskItemProps> = ({
  notification,
  children,
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
        <b>{notification.originatingUser?.name}</b>
        {'\u205F'}
        {children}
      </Box>
      <Box>
        <StyledLinkedDetailItem>
          <FlagImage
            flagEmoji={notification.linkedTask!.language.flagEmoji!}
            height={20}
            style={{ marginBottom: -5 }}
          />
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
