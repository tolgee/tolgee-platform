import { default as React, FunctionComponent } from 'react';
import { Box, ListItemButton, styled } from '@mui/material';
import { useHistory } from 'react-router-dom';
import { T } from '@tolgee/react';
import { components } from 'tg.service/apiSchema.generated';
import { useCurrentLanguage } from 'tg.hooks/useCurrentLanguage';
import { locales } from '../../../locales';
import { formatDistanceToNowStrict } from 'date-fns';
import { AvatarImg } from 'tg.component/common/avatar/AvatarImg';

const Item = styled(ListItemButton)`
  display: grid;
  column-gap: 10px;
  grid-template-columns: 30px 1fr 120px;
  grid-template-rows: 1fr;
  grid-template-areas:
    'notification-avatar notification-text notification-time'
    'notification-avatar notification-linked-detail notification-project';
  line-height: 1.3;
`;

const Avatar = styled(Box)`
  grid-area: notification-avatar;
`;

const Time = styled(Box)`
  font-size: 13px;
  grid-area: notification-time;
  text-align: right;
  color: ${({ theme }) =>
    theme.palette.mode === 'light'
      ? theme.palette.emphasis[400]
      : theme.palette.emphasis[600]};
`;

const Project = styled(Time)`
  grid-area: notification-project;
`;

const Text = styled(Box)`
  grid-area: notification-text;
`;

const Detail = styled(Box)`
  grid-area: notification-linked-detail;
`;

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

function getLocalizedMessage(
  notification: components['schemas']['NotificationModel']
) {
  switch (notification.type) {
    case 'TASK_ASSIGNED':
      return <T keyName="notifications-task-assigned" />;
    case 'TASK_COMPLETED':
      return <T keyName="notifications-task-completed" />;
    case 'MFA_ENABLED':
      return <T keyName="notifications-mfa-enabled" />;
    case 'MFA_DISABLED':
      return <T keyName="notifications-mfa-disabled" />;
    case 'PASSWORD_CHANGED':
      return <T keyName="notifications-password-changed" />;
  }
}

export const NotificationItem: FunctionComponent<{
  notification: components['schemas']['NotificationModel'];
  key: number;
  isLast: boolean;
}> = ({ notification, key, isLast }) => {
  const history = useHistory();
  const language = useCurrentLanguage();

  const destinationUrl = notification.type.startsWith('TASK_')
    ? `/projects/${notification.project?.id}/task?number=${notification.linkedTask?.number}`
    : '/account/security';
  const createdAt = notification.createdAt;
  const originatingUser = notification.originatingUser;
  const project = notification.project;
  return (
    <Item
      key={key}
      divider={!isLast}
      //@ts-ignore
      href={destinationUrl}
      onClick={(event) => {
        event.preventDefault();
        history.push(destinationUrl);
      }}
      data-cy="notifications-list-item"
    >
      <Avatar>
        {originatingUser && (
          <AvatarImg
            owner={{
              name: originatingUser.name,
              avatar: originatingUser.avatar,
              type: 'USER',
              id: originatingUser.id || 0,
            }}
            size={30}
          />
        )}
      </Avatar>
      <Text>
        <b>{originatingUser?.name}</b>&nbsp;
        {getLocalizedMessage(notification)}
      </Text>
      {notification.type.startsWith('TASK_') && (
        <Detail>
          <LinkedDetailItem>
            {notification.linkedTask?.language.flagEmoji}
          </LinkedDetailItem>
          <LinkedDetailItem>{notification.linkedTask?.name}</LinkedDetailItem>
          <LinkedDetailNumber>
            #{notification.linkedTask?.number}
          </LinkedDetailNumber>
        </Detail>
      )}
      {createdAt && (
        <Time>
          {formatDistanceToNowStrict(new Date(createdAt), {
            addSuffix: true,
            locale: locales[language].dateFnsLocale,
          })}
        </Time>
      )}
      {project && <Project>{project.name}</Project>}
    </Item>
  );
};
