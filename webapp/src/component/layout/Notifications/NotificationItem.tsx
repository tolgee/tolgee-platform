import { default as React } from 'react';
import {
  Box,
  ListItemButton,
  ListItemButtonProps,
  styled,
  Typography,
} from '@mui/material';
import { Link } from 'react-router-dom';
import { components } from 'tg.service/apiSchema.generated';
import { useCurrentLanguage } from 'tg.hooks/useCurrentLanguage';
import { locales } from '../../../locales';
import { formatDistanceToNowStrict } from 'date-fns';
import { AvatarImg } from 'tg.component/common/avatar/AvatarImg';

const StyledItem = styled(ListItemButton)`
  display: grid;
  column-gap: 10px;
  grid-template-columns: 32px 1fr 120px;
  grid-template-rows: 1fr;
  grid-template-areas:
    'notification-avatar notification-detail notification-time'
    'notification-avatar notification-detail notification-project';
  line-height: 1.3;
`;

const StyledDetail = styled(Box)`
  grid-area: notification-detail;
  font-size: 14px;

  b {
    font-weight: 500;
  }
`;

const StyledAvatar = styled(Box)`
  grid-area: notification-avatar;
`;

const StyledTime = styled(Box)`
  grid-area: notification-time;
  text-align: right;
`;

const StyledRightDetailText = styled(Typography)`
  font-size: 12px;
  color: ${({ theme }) => theme.palette.text.secondary};
`;

const StyledProject = styled(StyledTime)`
  grid-area: notification-project;
`;

export type NotificationItemProps = {
  notification: components['schemas']['NotificationModel'];
  destinationUrl?: string;
} & ListItemButtonProps;

export const NotificationItem: React.FC<NotificationItemProps> = ({
  notification,
  key,
  destinationUrl,
  children,
}) => {
  const language = useCurrentLanguage();
  const createdAt = notification.createdAt;
  const originatingUser = notification.originatingUser;
  const project = notification.project;
  return (
    <StyledItem
      key={key}
      //@ts-ignore
      component={Link}
      to={destinationUrl}
      data-cy="notifications-list-item"
    >
      <StyledAvatar>
        {originatingUser && (
          <AvatarImg
            owner={{
              name: originatingUser.name,
              avatar: originatingUser.avatar,
              type: 'USER',
              id: originatingUser.id || 0,
            }}
            size={32}
          />
        )}
      </StyledAvatar>
      <StyledDetail>{children}</StyledDetail>
      {createdAt && (
        <StyledTime>
          <StyledRightDetailText variant="body2">
            {formatDistanceToNowStrict(new Date(createdAt), {
              addSuffix: true,
              locale: locales[language].dateFnsLocale,
            })}
          </StyledRightDetailText>
        </StyledTime>
      )}
      {project && (
        <StyledProject>
          <StyledRightDetailText variant="body2">
            {project.name}
          </StyledRightDetailText>
        </StyledProject>
      )}
    </StyledItem>
  );
};
