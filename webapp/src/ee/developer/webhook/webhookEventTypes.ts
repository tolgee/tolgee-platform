import { useTranslate } from '@tolgee/react';
import { components } from 'tg.service/apiSchema.generated';

export type WebhookEventType = NonNullable<
  components['schemas']['WebhookConfigRequest']['eventTypes']
>[number];

export const WEBHOOK_EVENT_TYPES: WebhookEventType[] = [
  'PROJECT_ACTIVITY',
  'CONTENT_DELIVERY_PUBLISH',
];

export function useWebhookEventTypeLabel() {
  const { t } = useTranslate();
  return (type: WebhookEventType) => {
    switch (type) {
      case 'PROJECT_ACTIVITY':
        return t('webhook_event_type_project_activity', 'Project activity');
      case 'CONTENT_DELIVERY_PUBLISH':
        return t(
          'webhook_event_type_content_delivery_publish',
          'Content delivery published'
        );
      default:
        return type;
    }
  };
}
