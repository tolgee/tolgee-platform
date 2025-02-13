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
import { AvatarImg } from 'tg.component/common/avatar/AvatarImg';
import { LoadingSkeleton } from 'tg.component/LoadingSkeleton';
import { useDateFormatter } from 'tg.hooks/useLocale';
import { useTimeDistance } from 'tg.hooks/useTimeDistance';

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
  const timeDistance = useTimeDistance();
  const createdAt = notification?.createdAt || '';
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
            <Tooltip
              title={formatDate(new Date(createdAt), {
                dateStyle: 'long',
                timeStyle: 'short',
              })}
            >
              <span>{timeDistance(createdAt)}</span>
            </Tooltip>
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

export const NotificationItemSkeleton: React.FC<ListItemButtonProps> = (
  props
) => {
  return (
    <StyledItem {...props}>
      <StyledAvatar>
        <LoadingSkeleton
          sx={{ height: 32, width: 32 }}
          variant="circular"
          animation="wave"
        />
      </StyledAvatar>
      <StyledDetail>
        <LoadingSkeleton sx={{ height: 14, width: 200 }} animation="wave" />
        <LoadingSkeleton sx={{ height: 14, width: 120 }} animation="wave" />
      </StyledDetail>
      <StyledTime>
        <LoadingSkeleton sx={{ height: 12, width: 60 }} animation="wave" />
      </StyledTime>
      <StyledProject>
        <LoadingSkeleton sx={{ height: 12, width: 80 }} animation="wave" />
      </StyledProject>
    </StyledItem>
  );
};
