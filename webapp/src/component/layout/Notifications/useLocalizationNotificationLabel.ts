import { useTranslate } from '@tolgee/react';
import { components } from 'tg.service/apiSchema.generated';

type NotificationType = components['schemas']['NotificationModel']['type'];

export const useLocalizationNotificationLabel = () => {
  const { t } = useTranslate();

  return (type: NotificationType): string | undefined => {
    switch (type) {
      case 'KEYS_ADDED':
        return t('notifications-keys-added', 'New keys added');
      case 'SOURCE_CHANGED':
        return t('notifications-source-changed', 'Base text changed');
      case 'STRINGS_TRANSLATED':
        return t('notifications-strings-translated', 'Strings translated');
      case 'STRINGS_REVIEWED':
        return t('notifications-strings-reviewed', 'Strings reviewed');
      case 'AUTOMATICALLY_TRANSLATED':
        return t('notifications-auto-translated', 'Auto-translated');
      default:
        return undefined;
    }
  };
};
