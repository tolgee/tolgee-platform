import { default as React, useEffect } from 'react';
import { List, ListItem, styled, Typography } from '@mui/material';
import Menu from '@mui/material/Menu';
import {
  useApiInfiniteQuery,
  useApiMutation,
} from 'tg.service/http/useQueryApi';
import { T } from '@tolgee/react';
import { useGlobalContext } from 'tg.globalContext/GlobalContext';
import { useUser } from 'tg.globalContext/helpers';
import { PopoverProps } from '@mui/material/Popover';
import { notificationComponents } from 'tg.component/layout/Notifications/NotificationTypeMap';
import { NotificationsChanged } from 'tg.websocket-client/WebsocketClient';
import { components } from 'tg.service/apiSchema.generated';
import { InfiniteData } from 'react-query';
import { useWindowSize } from 'usehooks-ts';
import { NotificationItem } from 'tg.component/layout/Notifications/NotificationItem';

type PagedModelNotificationModel =
  components['schemas']['PagedModelNotificationModel'];

const FETCH_NEXT_PAGE_SCROLL_THRESHOLD_IN_PIXELS = 220;

const StyledMenu = styled(Menu)`
  .MuiPaper-root {
    margin-top: 5px;
  }
`;

const StyledHeader = styled(Typography)`
  font-size: 16px;
`;

function getNotifications(
  data: InfiniteData<PagedModelNotificationModel> | undefined
) {
  return data?.pages
    .flatMap((it) => it?._embedded?.notificationModelList)
    .filter((it) => it !== undefined);
}

type NotificationsPopupProps = {
  onClose: () => void;
  onNotificationsChanged: (event: NotificationsChanged) => void;
  anchorEl: PopoverProps['anchorEl'];
};

export const NotificationsPopup: React.FC<NotificationsPopupProps> = ({
  onClose,
  onNotificationsChanged,
  anchorEl,
}) => {
  const user = useUser();
  const client = useGlobalContext((c) => c.wsClient.client);

  const query = { size: 10 };
  const notificationsLoadable = useApiInfiniteQuery({
    url: '/v2/notifications',
    method: 'get',
    query: query,
    options: {
      enabled: !!anchorEl,
      refetchOnMount: false,
      staleTime: Infinity,
      cacheTime: Infinity,
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
      onSuccess(data) {
        const markAsSeenIds = getNotifications(data)?.map((it) => it.id);
        if (!markAsSeenIds) return;

        markSeenMutation.mutate({
          content: {
            'application/json': {
              notificationIds: markAsSeenIds,
            },
          },
        });
      },
    },
  });

  const markSeenMutation = useApiMutation({
    url: '/v2/notifications-mark-seen',
    method: 'put',
  });

  useEffect(() => {
    if (client && user) {
      return client.subscribe(
        `/users/${user.id}/notifications-changed`,
        (event) => {
          if (event.data.newNotification) {
            notificationsLoadable.remove();
          }
          onNotificationsChanged(event);
        }
      );
    }
  }, [user, client]);

  const windowSize = useWindowSize();
  const maxHeight = Math.min(500, windowSize.height - 100);
  const notifications = getNotifications(notificationsLoadable.data);

  return (
    <StyledMenu
      keepMounted
      open={Boolean(anchorEl)}
      anchorEl={anchorEl}
      onClose={onClose}
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
            maxHeight: maxHeight,
            minWidth: 400,
          },
          onScroll: (event) => {
            const target = event.target as HTMLDivElement;
            if (
              notificationsLoadable?.hasNextPage &&
              !notificationsLoadable.isFetching &&
              target.scrollHeight - target.clientHeight - target.scrollTop <
                FETCH_NEXT_PAGE_SCROLL_THRESHOLD_IN_PIXELS
            ) {
              notificationsLoadable.fetchNextPage();
            }
          },
        },
      }}
    >
      <List id="notifications-list" data-cy="notifications-list">
        <ListItem>
          <StyledHeader variant="h6">
            <T keyName="notifications-header" />
          </StyledHeader>
        </ListItem>
        {notifications?.map((notification, i) => {
          const Component = notificationComponents[notification.type]!;
          return (
            <Component notification={notification} key={notification.id} />
          );
        })}
        {notifications?.length === 0 && (
          <ListItem data-cy="notifications-empty-message">
            <T keyName="notifications-empty" />
          </ListItem>
        )}
        {[1, 2, 3, 4, 5].map(
          (i) =>
            (notificationsLoadable.isFetching ||
              notificationsLoadable.hasNextPage) && (
              <NotificationItem key={-i} />
            )
        )}
      </List>
    </StyledMenu>
  );
};
