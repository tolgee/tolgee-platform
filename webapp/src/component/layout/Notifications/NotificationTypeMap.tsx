import { TaskAssignedItem } from 'tg.component/layout/Notifications/TaskAssignedItem';
import { TaskFinishedItem } from 'tg.component/layout/Notifications/TaskFinishedItem';
import { TaskCanceledItem } from 'tg.component/layout/Notifications/TaskCanceledItem';
import { MfaEnabledItem } from 'tg.component/layout/Notifications/MfaEnabledItem';
import { MfaDisabledItem } from 'tg.component/layout/Notifications/MfaDisabledItem';
import { PasswordChangedItem } from 'tg.component/layout/Notifications/PasswordChangedItem';
import { components } from 'tg.service/apiSchema.generated';
import { NotificationItemProps } from 'tg.component/layout/Notifications/NotificationItem';
import React from 'react';
import { useTranslate } from '@tolgee/react';
import { LocalizationActionLabel } from 'tg.component/layout/Notifications/LocalizationActionLabel';
import { LocalizationItem } from 'tg.component/layout/Notifications/LocalizationItem';
import { CommentMentionItem } from 'tg.component/layout/Notifications/CommentMentionItem';

const LocalizationTypeItem = (props: NotificationItemProps) => {
  const { t } = useTranslate();
  const { notification } = props;
  const name =
    notification.originatingUser?.name ??
    t('notifications-unknown-author', 'Someone');
  return (
    <LocalizationItem
      {...props}
      label={<LocalizationActionLabel type={notification.type} name={name} />}
    />
  );
};

type NotificationsComponentMap = Record<
  components['schemas']['NotificationModel']['type'],
  React.FC<React.PropsWithChildren<NotificationItemProps>>
>;

export const notificationComponents = {
  TASK_ASSIGNED: TaskAssignedItem,
  TASK_FINISHED: TaskFinishedItem,
  TASK_CANCELED: TaskCanceledItem,
  MFA_ENABLED: MfaEnabledItem,
  MFA_DISABLED: MfaDisabledItem,
  PASSWORD_CHANGED: PasswordChangedItem,
  COMMENT_MENTION: CommentMentionItem,
  KEYS_ADDED: LocalizationTypeItem,
  IMPORTED: LocalizationTypeItem,
  SOURCE_CHANGED: LocalizationTypeItem,
  STRINGS_TRANSLATED: LocalizationTypeItem,
  STRINGS_REVIEWED: LocalizationTypeItem,
  AUTOMATICALLY_TRANSLATED: LocalizationTypeItem,
  // shortcut: prototype-only types without backend support
  ...({
    SUGGESTION_RESOLVED: LocalizationTypeItem,
    SUGGESTION_ADDED: LocalizationTypeItem,
  } as Partial<NotificationsComponentMap>),
} as NotificationsComponentMap;
