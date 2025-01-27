import { default as React, FunctionComponent } from 'react';
import {
  Box,
  ListItemButton,
  ListItemButtonProps,
  styled,
} from '@mui/material';
import { useHistory } from 'react-router-dom';
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
    'notification-avatar notification-detail notification-time'
    'notification-avatar notification-detail notification-project';
  line-height: 1.3;
`;

const Detail = styled(Box)`
  grid-area: notification-detail;
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

export type NotificationItemProps = {
  notification: components['schemas']['NotificationModel'];
  isLast: boolean;
  destinationUrl?: string;
} & ListItemButtonProps;

export const NotificationItem: FunctionComponent<NotificationItemProps> = ({
  notification,
  key,
  isLast,
  destinationUrl,
  children,
}) => {
  const history = useHistory();
  const language = useCurrentLanguage();
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
        if (!destinationUrl) return;
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
      <Detail>{children}</Detail>
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
