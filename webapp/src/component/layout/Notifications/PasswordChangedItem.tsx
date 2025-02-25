import { default as React, FunctionComponent } from 'react';
import { T } from '@tolgee/react';
import {
  NotificationItem,
  NotificationItemProps,
} from 'tg.component/layout/Notifications/NotificationItem';
import { LINKS } from 'tg.constants/links';

type PasswordChangedItemProps = NotificationItemProps;

export const PasswordChangedItem: FunctionComponent<
  PasswordChangedItemProps
> = ({ notification, ...props }) => {
  return (
    <NotificationItem
      notification={notification}
      destinationUrl={LINKS.USER_ACCOUNT_SECURITY.build()}
      {...props}
    >
      <b>{notification.originatingUser?.name}</b>&nbsp;
      <T keyName="notifications-password-changed" />
    </NotificationItem>
  );
};
