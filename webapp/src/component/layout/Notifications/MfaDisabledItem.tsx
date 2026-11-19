import { default as React, FunctionComponent } from 'react';
import { T } from '@tolgee/react';
import { ShieldOff } from '@untitled-ui/icons-react';
import { NotificationItemProps } from 'tg.component/layout/Notifications/NotificationItem';
import { SecurityItem } from 'tg.component/layout/Notifications/SecurityItem';
import { NotificationUserName } from 'tg.component/layout/Notifications/NotificationUserName';

export const MfaDisabledItem: FunctionComponent<
  React.PropsWithChildren<NotificationItemProps>
> = (props) => {
  return (
    <SecurityItem
      {...props}
      icon={<ShieldOff />}
      renderLabel={(name) => (
        <T
          keyName="notifications-security-mfa-disabled-by"
          defaultValue="<b>{name}</b> disabled two-factor sign-in"
          params={{ name, b: <NotificationUserName name={name} /> }}
        />
      )}
    />
  );
};
