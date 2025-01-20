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
import { useApiMutation, useApiQuery } from 'tg.service/http/useQueryApi';
import { Bell01 } from '@untitled-ui/icons-react';
import { T } from '@tolgee/react';
import { useGlobalContext } from 'tg.globalContext/GlobalContext';
import { useUser } from 'tg.globalContext/helpers';
import { components } from 'tg.service/apiSchema.generated';
import { useCurrentLanguage } from 'tg.hooks/useCurrentLanguage';
import { locales } from '../../../locales';
import { formatDistanceToNowStrict } from 'date-fns';

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
  grid-template-columns: 1fr auto;
  grid-template-rows: auto;
  grid-template-areas: 'notification-text notification-time';
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

export const Notifications: FunctionComponent<{ className?: string }> = () => {
  const history = useHistory();
  const client = useGlobalContext((c) => c.wsClient.client);
  const user = useUser();
  const language = useCurrentLanguage();

  const [anchorEl, setAnchorEl] = useState(null);
  const [notifications, setNotifications] = useState<
    components['schemas']['NotificationModel'][]
  >([]);
  const [unseenCount, setUnseenCount] = useState(0);

  const notificationsLoadable = useApiQuery({
    url: '/v2/notifications',
    method: 'get',
    query: { size: 10000 },
  });

  const markSeenMutation = useApiMutation({
    url: '/v2/notifications-mark-seen',
    method: 'put',
  });

  const handleOpen = (event: React.MouseEvent<HTMLButtonElement>) => {
    // @ts-ignore
    setAnchorEl(event.currentTarget);
    markSeenMutation.mutate({
      content: {
        'application/json':
          notifications != undefined ? notifications.map((it) => it.id) : [],
      },
    });
  };

  const handleClose = () => {
    setAnchorEl(null);
  };

  useEffect(() => {
    const notificationsLoaded = notificationsLoadable.data;
    if (notificationsLoaded === undefined) return;
    setNotifications(
      notificationsLoaded._embedded?.notificationModelList || []
    );
    setUnseenCount(notificationsLoaded.unseenCount);
  }, [notificationsLoadable.data]);

  useEffect(() => {
    if (client && user) {
      return client.subscribe(
        `/users/${user.id}/notifications-changed`,
        (e) => {
          const newNotification = e.data.newNotification;
          if (newNotification != undefined)
            setNotifications((prevState) => [newNotification, ...prevState]);
          setUnseenCount((prevState) => e.data.currentlyUnseenCount);
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
          data-cy="notifications-count"
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
            const createdAt = notification.createdAt;
            return (
              <NotificationItem
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
                <Box sx={{ gridArea: 'notification-text' }}>
                  <T
                    keyName="notifications-task-assigned"
                    params={{ taskName: notification.linkedTask?.name }}
                  />
                </Box>
                <NotificationItemTime>
                  {createdAt &&
                    formatDistanceToNowStrict(new Date(createdAt), {
                      addSuffix: true,
                      locale: locales[language].dateFnsLocale,
                    })}
                </NotificationItemTime>
              </NotificationItem>
            );
          })}
          {notifications?.length === 0 && (
            <ListItem data-cy="notifications-empty-message">
              <T keyName="notifications-empty" />
            </ListItem>
          )}
        </List>
      </StyledMenu>
    </>
  );
};
