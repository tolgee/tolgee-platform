import { TaskAssignedItem } from 'tg.component/layout/Notifications/TaskAssignedItem';
import { TaskFinishedItem } from 'tg.component/layout/Notifications/TaskFinishedItem';
import { TaskCanceledItem } from 'tg.component/layout/Notifications/TaskCanceledItem';
import { MfaEnabledItem } from 'tg.component/layout/Notifications/MfaEnabledItem';
import { MfaDisabledItem } from 'tg.component/layout/Notifications/MfaDisabledItem';
import { PasswordChangedItem } from 'tg.component/layout/Notifications/PasswordChangedItem';
import { components } from 'tg.service/apiSchema.generated';
import { NotificationItemProps } from 'tg.component/layout/Notifications/NotificationItem';
import React from 'react';
import { useLocalizationNotificationLabel } from 'tg.component/layout/Notifications/useLocalizationNotificationLabel';
import { LocalizationItem } from 'tg.component/layout/Notifications/LocalizationItem';
import { CommentMentionItem } from 'tg.component/layout/Notifications/CommentMentionItem';

const LocalizationTypeItem = (props: NotificationItemProps) => {
  const getLabel = useLocalizationNotificationLabel();
  return (
    <LocalizationItem
      {...props}
      label={getLabel(props.notification.type) ?? ''}
    />
  );
};

type NotificationsComponentMap = Record<
  components['schemas']['NotificationModel']['type'],
  React.FC<React.PropsWithChildren<NotificationItemProps>>
>;

export const notificationComponents: NotificationsComponentMap = {
  TASK_ASSIGNED: TaskAssignedItem,
  TASK_FINISHED: TaskFinishedItem,
  TASK_CANCELED: TaskCanceledItem,
  MFA_ENABLED: MfaEnabledItem,
  MFA_DISABLED: MfaDisabledItem,
  PASSWORD_CHANGED: PasswordChangedItem,
  COMMENT_MENTION: CommentMentionItem,
  KEYS_ADDED: LocalizationTypeItem,
  SOURCE_CHANGED: LocalizationTypeItem,
  STRINGS_TRANSLATED: LocalizationTypeItem,
  STRINGS_REVIEWED: LocalizationTypeItem,
  AUTOMATICALLY_TRANSLATED: LocalizationTypeItem,
};
