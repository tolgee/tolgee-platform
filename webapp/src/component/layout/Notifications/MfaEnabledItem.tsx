import { default as React, FunctionComponent } from 'react';
import { T } from '@tolgee/react';
import {
  NotificationItem,
  NotificationItemProps,
} from 'tg.component/layout/Notifications/NotificationItem';

type MfaEnabledItemProps = NotificationItemProps;

export const MfaEnabledItem: FunctionComponent<MfaEnabledItemProps> = ({
  notification,
  ...props
}) => {
  const destinationUrl = '/account/security';
  return (
    <NotificationItem
      notification={notification}
      destinationUrl={destinationUrl}
      {...props}
    >
      <b>{notification.originatingUser?.name}</b>&nbsp;
      <T keyName="notifications-mfa-enabled" />
    </NotificationItem>
  );
};
