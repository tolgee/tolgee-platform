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
import { LocalizationItem } from 'tg.component/layout/Notifications/LocalizationItem';

const KeysAddedItem = (props: NotificationItemProps) => {
  const { t } = useTranslate();
  return (
    <LocalizationItem
      {...props}
      label={t('notifications-keys-added', 'New keys added')}
    />
  );
};

const SourceChangedItem = (props: NotificationItemProps) => {
  const { t } = useTranslate();
  return (
    <LocalizationItem
      {...props}
      label={t('notifications-source-changed', 'Base text changed')}
    />
  );
};

const StringsTranslatedItem = (props: NotificationItemProps) => {
  const { t } = useTranslate();
  return (
    <LocalizationItem
      {...props}
      label={t('notifications-strings-translated', 'Strings translated')}
    />
  );
};

const StringsReviewedItem = (props: NotificationItemProps) => {
  const { t } = useTranslate();
  return (
    <LocalizationItem
      {...props}
      label={t('notifications-strings-reviewed', 'Strings reviewed')}
    />
  );
};

const AutomaticallyTranslatedItem = (props: NotificationItemProps) => {
  const { t } = useTranslate();
  return (
    <LocalizationItem
      {...props}
      label={t(
        'notifications-automatically-translated',
        'Automatically translated'
      )}
    />
  );
};

const BulkChangedItem = (props: NotificationItemProps) => {
  const { t } = useTranslate();
  return (
    <LocalizationItem
      {...props}
      label={t('notifications-bulk-changed', 'Changed in bulk')}
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
  KEYS_ADDED: KeysAddedItem,
  SOURCE_CHANGED: SourceChangedItem,
  STRINGS_TRANSLATED: StringsTranslatedItem,
  STRINGS_REVIEWED: StringsReviewedItem,
  AUTOMATICALLY_TRANSLATED: AutomaticallyTranslatedItem,
  BULK_CHANGED: BulkChangedItem,
};
