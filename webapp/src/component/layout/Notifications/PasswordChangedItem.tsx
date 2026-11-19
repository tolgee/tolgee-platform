import { default as React, FunctionComponent } from 'react';
import { T } from '@tolgee/react';
import { Lock01 } from '@untitled-ui/icons-react';
import { NotificationItemProps } from 'tg.component/layout/Notifications/NotificationItem';
import { SecurityItem } from 'tg.component/layout/Notifications/SecurityItem';
import { NotificationUserName } from 'tg.component/layout/Notifications/NotificationUserName';

export const PasswordChangedItem: FunctionComponent<
  React.PropsWithChildren<NotificationItemProps>
> = (props) => {
  return (
    <SecurityItem
      {...props}
      icon={<Lock01 />}
      renderLabel={(name) => (
        <T
          keyName="notifications-security-password-changed-by"
          defaultValue="<b>{name}</b> changed the password"
          params={{ name, b: <NotificationUserName name={name} /> }}
        />
      )}
    />
  );
};
