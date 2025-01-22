import {
  default as React,
  FunctionComponent,
  useEffect,
  useState,
} from 'react';
import {
  Badge,
  IconButton,
  List,
  ListItem,
  ListItemButton,
  styled,
} from '@mui/material';
import Menu from '@mui/material/Menu';
import { useHistory } from 'react-router-dom';
import { useApiMutation, useApiQuery } from 'tg.service/http/useQueryApi';
import { Bell01 } from '@untitled-ui/icons-react';
import { T } from '@tolgee/react';
import { useGlobalContext } from 'tg.globalContext/GlobalContext';
import { useUser } from 'tg.globalContext/helpers';
import { components } from 'tg.service/apiSchema.generated';

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

export const Notifications: FunctionComponent<{ className?: string }> = () => {
  const history = useHistory();
  const user = useUser();
  const client = useGlobalContext((c) => c.wsClient.client);

  const [anchorEl, setAnchorEl] = useState(null);
  const [notifications, setNotifications] = useState<
    components['schemas']['NotificationModel'][] | undefined
  >(undefined);
  const [unseenCount, setUnseenCount] = useState<number | undefined>(undefined);

  const unseenNotificationsLoadable = useApiQuery({
    url: '/v2/notifications',
    method: 'get',
    query: { size: 1, filterSeen: false },
  });

  const notificationsLoadable = useApiQuery({
    url: '/v2/notifications',
    method: 'get',
    query: { size: 10000 },
    options: { enabled: false },
  });

  const markSeenMutation = useApiMutation({
    url: '/v2/notifications-mark-seen',
    method: 'put',
  });

  const handleOpen = (event: React.MouseEvent<HTMLButtonElement>) => {
    if (!notifications) {
      notificationsLoadable.refetch();
    }
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
    if (notifications !== undefined) return;
    setNotifications(
      notificationsLoadable.data?._embedded?.notificationModelList
    );
  }, [notificationsLoadable.data]);

  useEffect(() => {
    if (!anchorEl || !notifications) return;

    markSeenMutation.mutate({
      content: {
        'application/json': {
          notificationIds: notifications.map((it) => it.id),
        },
      },
    });
  }, [notifications, anchorEl]);

  useEffect(() => {
    if (client && user) {
      return client.subscribe(
        `/users/${user.id}/notifications-changed`,
        (e) => {
          setUnseenCount(e.data.currentlyUnseenCount);
          const newNotification = e.data.newNotification;
          if (newNotification)
            setNotifications((prevState) =>
              prevState ? [newNotification, ...prevState] : prevState
            );
          unseenNotificationsLoadable.remove();
          notificationsLoadable.remove();
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
              maxHeight: 400,
            },
          },
        }}
      >
        <List id="notifications-list" data-cy="notifications-list">
          <ListItemHeader divider>
            <T keyName="notifications-header" />
          </ListItemHeader>
          {notifications?.map((notification, i) => {
            const destinationUrl = `/projects/${notification.project?.id}/task?number=${notification.linkedTask?.number}`;
            return (
              <ListItemButton
                key={notification.id}
                divider={i !== notifications.length - 1}
                href={destinationUrl}
                onClick={(event) => {
                  event.preventDefault();
                  handleClose();
                  history.push(destinationUrl);
                }}
                data-cy="notifications-list-item"
              >
                <T
                  keyName="notifications-task-assigned"
                  params={{ taskName: notification.linkedTask?.name }}
                />
              </ListItemButton>
            );
          })}
          {!notifications?.length && (
            <ListItem data-cy="notifications-empty-message">
              <T keyName="notifications-empty" />
            </ListItem>
          )}
        </List>
      </StyledMenu>
    </>
  );
};
