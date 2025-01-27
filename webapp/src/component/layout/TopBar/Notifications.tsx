import {
  default as React,
  FunctionComponent,
  useEffect,
  useState,
} from 'react';
import {
  Badge,
  Box,
  IconButton,
  List,
  ListItem,
  ListItemButton,
  styled,
} from '@mui/material';
import Menu from '@mui/material/Menu';
import { useHistory } from 'react-router-dom';
import {
  useApiInfiniteQuery,
  useApiMutation,
  useApiQuery,
} from 'tg.service/http/useQueryApi';
import { Bell01 } from '@untitled-ui/icons-react';
import { T } from '@tolgee/react';
import { useGlobalContext } from 'tg.globalContext/GlobalContext';
import { useUser } from 'tg.globalContext/helpers';
import { components } from 'tg.service/apiSchema.generated';
import { useCurrentLanguage } from 'tg.hooks/useCurrentLanguage';
import { locales } from '../../../locales';
import { formatDistanceToNowStrict } from 'date-fns';
import { AvatarImg } from 'tg.component/common/avatar/AvatarImg';
import { BoxLoading } from 'tg.component/common/BoxLoading';

const StyledMenu = styled(Menu)`
  .MuiPaper-root {
    margin-top: 5px;
  }
`;

const StyledIconButton = styled(IconButton)`
  width: 40px;
  height: 40px;

  img {
    user-drag: none;
  }
`;

const ListItemHeader = styled(ListItem)`
  font-weight: bold;
`;

const NotificationItem = styled(ListItemButton)`
  display: grid;
  column-gap: 10px;
  grid-template-columns: 30px 1fr 120px;
  grid-template-rows: 1fr;
  grid-template-areas:
    'notification-avatar notification-text notification-time'
    'notification-avatar notification-linked-detail notification-project';
  line-height: 1.3;
`;

const NotificationAvatar = styled(Box)`
  grid-area: notification-avatar;
`;

const NotificationItemTime = styled(Box)`
  font-size: 13px;
  grid-area: notification-time;
  text-align: right;
  color: ${({ theme }) =>
    theme.palette.mode === 'light'
      ? theme.palette.emphasis[400]
      : theme.palette.emphasis[600]};
`;

const NotificationItemProject = styled(NotificationItemTime)`
  grid-area: notification-project;
`;

const NotificationItemText = styled(Box)`
  grid-area: notification-text;
`;

const NotificationItemLinkedDetail = styled(Box)`
  grid-area: notification-linked-detail;
`;

const NotificationItemLinkedDetailItem = styled(Box)`
  margin-right: 10px;
  display: inline;
`;

const NotificationItemLinkedDetailNumber = styled(
  NotificationItemLinkedDetailItem
)`
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

export const Notifications: FunctionComponent<{ className?: string }> = () => {
  const history = useHistory();
  const user = useUser();
  const language = useCurrentLanguage();
  const client = useGlobalContext((c) => c.wsClient.client);

  const [anchorEl, setAnchorEl] = useState(null);
  const [unseenCount, setUnseenCount] = useState<number | undefined>(undefined);

  const unseenNotificationsLoadable = useApiQuery({
    url: '/v2/notifications',
    method: 'get',
    query: { size: 1, filterSeen: false },
    options: {
      enabled: unseenCount === undefined,
      refetchOnMount: false,
      keepPreviousData: true,
    },
  });

  const query = { size: 10 };
  const notificationsLoadable = useApiInfiniteQuery({
    url: '/v2/notifications',
    method: 'get',
    query: query,
    options: {
      enabled: false,
      getNextPageParam: (lastPage) => {
        if (
          lastPage.page &&
          lastPage.page.number! < lastPage.page.totalPages! - 1
        ) {
          return {
            query: {
              ...query,
              page: lastPage.page!.number! + 1,
            },
          };
        } else {
          return null;
        }
      },
    },
  });

  const markSeenMutation = useApiMutation({
    url: '/v2/notifications-mark-seen',
    method: 'put',
  });

  const notifications = notificationsLoadable.data?.pages
    .flatMap((it) => it?._embedded?.notificationModelList)
    .filter((it) => it !== undefined);

  const handleOpen = (event: React.MouseEvent<HTMLButtonElement>) => {
    // @ts-ignore
    setAnchorEl(event.currentTarget);
  };

  const handleClose = () => {
    setAnchorEl(null);
  };

  useEffect(() => {
    if (unseenCount !== undefined) return;
    setUnseenCount(
      (prevState) =>
        unseenNotificationsLoadable.data?.page?.totalElements || prevState
    );
  }, [unseenNotificationsLoadable.data]);

  useEffect(() => {
    if (!anchorEl) return;

    const data = notificationsLoadable.data;
    if (!data) {
      if (!notificationsLoadable.isFetching) {
        notificationsLoadable.refetch();
      }
      return;
    }

    const markAsSeenIds = notifications?.map((it) => it.id);
    if (!markAsSeenIds) return;

    markSeenMutation.mutate({
      content: {
        'application/json': {
          notificationIds: markAsSeenIds,
        },
      },
    });
  }, [notificationsLoadable.data, anchorEl]);

  useEffect(() => {
    if (client && user) {
      return client.subscribe(
        `/users/${user.id}/notifications-changed`,
        (e) => {
          setUnseenCount(e.data.currentlyUnseenCount);
          const newNotification = e.data.newNotification;
          if (newNotification) notificationsLoadable.remove();
          unseenNotificationsLoadable.remove();
        }
      );
    }
  }, [user, client]);

  return (
    <>
      <StyledIconButton
        color="inherit"
        aria-controls="notifications-button"
        aria-haspopup="true"
        data-cy="notifications-button"
        onClick={handleOpen}
        size="large"
      >
        <Badge
          badgeContent={unseenCount}
          color="secondary"
          slotProps={{
            badge: {
              //@ts-ignore
              'data-cy': 'notifications-count',
            },
          }}
        >
          <Bell01 />
        </Badge>
      </StyledIconButton>
      <StyledMenu
        keepMounted
        open={!!anchorEl}
        anchorEl={anchorEl}
        onClose={handleClose}
        anchorOrigin={{
          vertical: 'bottom',
          horizontal: 'right',
        }}
        transformOrigin={{
          vertical: 'top',
          horizontal: 'right',
        }}
        slotProps={{
          paper: {
            style: {
              maxHeight: 500,
              minWidth: 400,
            },
            onScroll: (event) => {
              const target = event.target as HTMLDivElement;
              if (
                notificationsLoadable?.hasNextPage &&
                !notificationsLoadable.isFetching &&
                target.scrollHeight - target.clientHeight - target.scrollTop <
                  100
              ) {
                notificationsLoadable.fetchNextPage();
              }
            },
          },
        }}
      >
        <List id="notifications-list" data-cy="notifications-list">
          <ListItemHeader divider>
            <T keyName="notifications-header" />
          </ListItemHeader>
          {notifications?.map((notification, i) => {
            const destinationUrl = notification.type.startsWith('TASK_')
              ? `/projects/${notification.project?.id}/task?number=${notification.linkedTask?.number}`
              : '/account/security';
            const createdAt = notification.createdAt;
            const originatingUser = notification.originatingUser;
            const project = notification.project;
            return (
              <NotificationItem
                key={notification.id}
                divider={i !== notifications.length - 1}
                //@ts-ignore
                href={destinationUrl}
                onClick={(event) => {
                  event.preventDefault();
                  handleClose();
                  history.push(destinationUrl);
                }}
                data-cy="notifications-list-item"
              >
                <NotificationAvatar>
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
                </NotificationAvatar>
                <NotificationItemText>
                  <b>{originatingUser?.name}</b>&nbsp;
                  {getLocalizedMessage(notification)}
                </NotificationItemText>
                {notification.type.startsWith('TASK_') && (
                  <NotificationItemLinkedDetail>
                    <NotificationItemLinkedDetailItem>
                      {notification.linkedTask?.language.flagEmoji}
                    </NotificationItemLinkedDetailItem>
                    <NotificationItemLinkedDetailItem>
                      {notification.linkedTask?.name}
                    </NotificationItemLinkedDetailItem>
                    <NotificationItemLinkedDetailNumber>
                      #{notification.linkedTask?.number}
                    </NotificationItemLinkedDetailNumber>
                  </NotificationItemLinkedDetail>
                )}
                {createdAt && (
                  <NotificationItemTime>
                    {formatDistanceToNowStrict(new Date(createdAt), {
                      addSuffix: true,
                      locale: locales[language].dateFnsLocale,
                    })}
                  </NotificationItemTime>
                )}
                {project && (
                  <NotificationItemProject>
                    {project.name}
                  </NotificationItemProject>
                )}
              </NotificationItem>
            );
          })}
          {notifications?.length === 0 && (
            <ListItem data-cy="notifications-empty-message">
              <T keyName="notifications-empty" />
            </ListItem>
          )}
          {(notificationsLoadable.isFetching ||
            notificationsLoadable.hasNextPage) && (
            <ListItem>
              <BoxLoading width="100%" />
            </ListItem>
          )}
        </List>
      </StyledMenu>
    </>
  );
};
