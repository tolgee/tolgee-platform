import { default as React, FunctionComponent } from 'react';
import { T } from '@tolgee/react';
import { ShieldTick } from '@untitled-ui/icons-react';
import { NotificationItemProps } from 'tg.component/layout/Notifications/NotificationItem';
import { SecurityItem } from 'tg.component/layout/Notifications/SecurityItem';
import { NotificationUserName } from 'tg.component/layout/Notifications/NotificationUserName';

export const MfaEnabledItem: FunctionComponent<
  React.PropsWithChildren<NotificationItemProps>
> = (props) => {
  return (
    <SecurityItem
      {...props}
      icon={<ShieldTick />}
      renderLabel={(name) => (
        <T
          keyName="notifications-security-mfa-enabled-by"
          defaultValue="<b>{name}</b> enabled two-factor sign-in"
          params={{ name, b: <NotificationUserName name={name} /> }}
        />
      )}
    />
  );
};
