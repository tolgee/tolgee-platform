import { default as React } from 'react';
import {
  Box,
  ListItemButton,
  ListItemButtonProps,
  styled,
  Tooltip,
  Typography,
} from '@mui/material';
import { Link } from 'react-router-dom';
import { components } from 'tg.service/apiSchema.generated';
import { useCurrentLanguage } from 'tg.hooks/useCurrentLanguage';
import { locales } from '../../../locales';
import { formatDistanceToNowStrict } from 'date-fns';
import { AvatarImg } from 'tg.component/common/avatar/AvatarImg';
import { LoadingSkeleton } from 'tg.component/LoadingSkeleton';
import { useDateFormatter } from 'tg.hooks/useLocale';

const StyledItem = styled(ListItemButton)`
  display: grid;
  column-gap: 10px;
  grid-template-columns: 32px 1fr 120px;
  grid-template-rows: auto auto;
  grid-template-areas:
    'notification-avatar notification-detail notification-time'
    'notification-avatar notification-detail notification-project';
  line-height: 1.3;
`;

const StyledDetail = styled(Box)`
  grid-area: notification-detail;
  font-size: 14px;
  word-break: break-word;
  overflow-wrap: break-word;

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
  display: flex;
  justify-content: flex-end;
`;

const StyledRightDetailText = styled(Typography)`
  font-size: 12px;
  color: ${({ theme }) => theme.palette.text.secondary};
`;

const StyledProject = styled(StyledTime)`
  grid-area: notification-project;
`;

export type NotificationItemProps = NotificationItemInternalProps & {
  notification: components['schemas']['NotificationModel'];
};

type NotificationItemInternalProps = {
  notification?: components['schemas']['NotificationModel'];
  destinationUrl?: string;
} & ListItemButtonProps;

export const NotificationItem: React.FC<NotificationItemInternalProps> = ({
  notification,
  key,
  destinationUrl,
  children,
}) => {
  const language = useCurrentLanguage();
  const createdAt = notification?.createdAt;
  const originatingUser = notification?.originatingUser;
  const project = notification?.project;
  const formatDate = useDateFormatter();
  return (
    <StyledItem
      key={key}
      //@ts-ignore
      component={Link}
      to={destinationUrl}
      data-cy="notifications-list-item"
    >
      <StyledAvatar>
        {notification ? (
          originatingUser && (
            <AvatarImg
              owner={{
                name: originatingUser.name,
                avatar: originatingUser.avatar,
                type: 'USER',
                id: originatingUser.id || 0,
              }}
              size={32}
            />
          )
        ) : (
          <LoadingSkeleton
            sx={{ height: 32, width: 32 }}
            variant="circular"
            animation="wave"
          />
        )}
      </StyledAvatar>
      <StyledDetail>
        {notification ? (
          children
        ) : (
          <>
            <LoadingSkeleton sx={{ height: 14, width: 200 }} animation="wave" />
            <LoadingSkeleton sx={{ height: 14, width: 120 }} animation="wave" />
          </>
        )}
      </StyledDetail>
      {notification ? (
        createdAt && (
          <StyledTime>
            <StyledRightDetailText variant="body2">
              <Tooltip
                title={formatDate(new Date(createdAt), {
                  dateStyle: 'long',
                  timeStyle: 'short',
                })}
              >
                <span>
                  {formatDistanceToNowStrict(new Date(createdAt), {
                    addSuffix: true,
                    locale: locales[language].dateFnsLocale,
                  })}
                </span>
              </Tooltip>
            </StyledRightDetailText>
          </StyledTime>
        )
      ) : (
        <StyledTime>
          <LoadingSkeleton sx={{ height: 12, width: 60 }} animation="wave" />
        </StyledTime>
      )}
      {notification ? (
        project && (
          <StyledProject>
            <StyledRightDetailText variant="body2">
              {project.name}
            </StyledRightDetailText>
          </StyledProject>
        )
      ) : (
        <StyledProject>
          <LoadingSkeleton sx={{ height: 12, width: 80 }} animation="wave" />
        </StyledProject>
      )}
    </StyledItem>
  );
};
