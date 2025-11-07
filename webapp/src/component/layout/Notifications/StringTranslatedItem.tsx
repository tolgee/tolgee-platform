import { default as React, FunctionComponent } from 'react';
import { T } from '@tolgee/react';
import {
  NotificationItem,
  NotificationItemProps,
} from 'tg.component/layout/Notifications/NotificationItem';
import { LINKS } from 'tg.constants/links';

type StringTranslatedItem = NotificationItemProps;

export const StringTranslatedItem: FunctionComponent<StringTranslatedItem> = ({
  notification,
  ...props
}) => {
  return (
    <NotificationItem
      notification={notification}
      destinationUrl={LINKS.PROJECT_TRANSLATIONS.build({
        projectId: notification.project!.id,
      })}
      {...props}
    >
      <b>{notification.originatingUser?.name}</b>
      {'\u205F'}
      <T keyName="notifications-string-translated" />
    </NotificationItem>
  );
};
