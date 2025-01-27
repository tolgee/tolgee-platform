import { default as React, FunctionComponent } from 'react';
import { T } from '@tolgee/react';
import {
  NotificationItem,
  NotificationItemProps,
} from 'tg.component/layout/Notifications/NotificationItem';

type MfaDisabledItemProps = NotificationItemProps;

export const MfaDisabledItem: FunctionComponent<MfaDisabledItemProps> = ({
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
      <T keyName="notifications-mfa-disabled" />
    </NotificationItem>
  );
};
