import { FunctionComponent } from 'react';
import { T } from '@tolgee/react';
import { components } from 'tg.service/apiSchema.generated';
import { NotificationUserName } from 'tg.component/layout/Notifications/NotificationUserName';

type NotificationType = components['schemas']['NotificationModel']['type'];

type ActionProps = {
  type: NotificationType;
  name: string;
};

export const LocalizationActionLabel: FunctionComponent<ActionProps> = ({
  type,
  name,
}) => {
  const params = { name, b: <NotificationUserName name={name} /> };
  switch (type) {
    case 'KEYS_ADDED':
      return (
        <T
          keyName="notifications-action-keys-added"
          defaultValue="<b>{name}</b> created"
          params={params}
        />
      );
    case 'IMPORTED':
      return (
        <T
          keyName="notifications-action-imported"
          defaultValue="<b>{name}</b> imported"
          params={params}
        />
      );
    case 'SOURCE_CHANGED':
      return (
        <T
          keyName="notifications-action-source-changed"
          defaultValue="<b>{name}</b> changed base text"
          params={params}
        />
      );
    case 'STRINGS_TRANSLATED':
      return (
        <T
          keyName="notifications-action-strings-translated"
          defaultValue="<b>{name}</b> translated"
          params={params}
        />
      );
    case 'STRINGS_REVIEWED':
      return (
        <T
          keyName="notifications-action-strings-reviewed"
          defaultValue="<b>{name}</b> reviewed"
          params={params}
        />
      );
    case 'AUTOMATICALLY_TRANSLATED':
      return (
        <T
          keyName="notifications-action-auto-translated"
          defaultValue="<b>{name}</b> auto-translated"
          params={params}
        />
      );
    case 'COMMENT_MENTION':
      return (
        <T
          keyName="notifications-action-mentioned-you"
          defaultValue="<b>{name}</b> mentioned you in a comment"
          params={params}
        />
      );
    default:
      return null;
  }
};

type CountProps = {
  type: NotificationType;
  count: number;
};

export const LocalizationCountLabel: FunctionComponent<CountProps> = ({
  type,
  count,
}) => {
  if (count >= 100) {
    return (
      <T keyName="notifications-count-keys-capped" defaultValue="100+ keys" />
    );
  }
  if (type === 'KEYS_ADDED') {
    return (
      <T
        keyName="notifications-count-new-keys"
        defaultValue="{count, plural, one {# new key} other {# new keys}}"
        params={{ count }}
      />
    );
  }
  return (
    <T
      keyName="notifications-count-keys"
      defaultValue="{count, plural, one {# key} other {# keys}}"
      params={{ count }}
    />
  );
};
