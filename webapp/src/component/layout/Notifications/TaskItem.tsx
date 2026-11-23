import { default as React, FunctionComponent } from 'react';
import { Box, styled } from '@mui/material';
import {
  NotificationItem,
  NotificationItemProps,
} from 'tg.component/layout/Notifications/NotificationItem';
import { getTaskUrl } from 'tg.constants/links';
import { FlagImage } from '@tginternal/library/components/languages/FlagImage';
import { useTranslate } from '@tolgee/react';
import { NotificationUserName } from 'tg.component/layout/Notifications/NotificationUserName';

const StyledDetail = styled(Box)`
  display: flex;
  align-items: center;
  gap: 6px;
  min-width: 0;
`;

const StyledTaskName = styled(Box)`
  color: ${({ theme }) => theme.palette.primary.main};
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
`;

const StyledTaskNumber = styled(Box)`
  flex-shrink: 0;
  color: ${({ theme }) => theme.palette.text.secondary};
`;

export type TaskItemProps = NotificationItemProps;

export const TaskItem: FunctionComponent<
  React.PropsWithChildren<TaskItemProps>
> = ({ notification, children, ...props }) => {
  const { t } = useTranslate();
  const task = notification.linkedTask!;
  const destinationUrl = getTaskUrl(notification.project!.id, task.number);
  return (
    <NotificationItem
      notification={notification}
      destinationUrl={destinationUrl}
      {...props}
    >
      <Box>
        <NotificationUserName name={notification.originatingUser?.name ?? ''} />
        {' '}
        {children}
      </Box>
      <StyledDetail>
        <StyledTaskName>{task.name || t('task_default_name')}</StyledTaskName>
        <StyledTaskNumber>#{task.number}</StyledTaskNumber>
        {task.language.flagEmoji && (
          <FlagImage
            flagEmoji={task.language.flagEmoji}
            height={16}
            style={{ flexShrink: 0 }}
          />
        )}
      </StyledDetail>
    </NotificationItem>
  );
};
