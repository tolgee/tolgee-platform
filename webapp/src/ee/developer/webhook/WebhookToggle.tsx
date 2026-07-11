import { T, useTranslate } from '@tolgee/react';
import { Switch, Tooltip } from '@mui/material';

import { components } from 'tg.service/apiSchema.generated';
import { useApiMutation } from 'tg.service/http/useQueryApi';
import { useProject } from 'tg.hooks/useProject';
import { confirmation } from 'tg.hooks/confirmation';

type WebhookConfigModel = components['schemas']['WebhookConfigModel'];

type Props = {
  data: WebhookConfigModel;
};

export const WebhookToggle = ({ data }: Props) => {
  const project = useProject();
  const { t } = useTranslate();

  const toggleWebhook = useApiMutation({
    url: '/v2/projects/{projectId}/webhook-configs/{id}',
    method: 'put',
    invalidatePrefix: '/v2/projects/{projectId}/webhook-configs',
  });

  function performToggle(enabled: boolean) {
    toggleWebhook.mutate({
      path: { projectId: project.id, id: data.id },
      content: {
        'application/json': {
          url: data.url,
          enabled,
        },
      },
    });
  }

  function handleToggle() {
    if (!data.enabled) {
      performToggle(true);
      return;
    }

    confirmation({
      title: (
        <T
          keyName="webhook_disable_confirmation_title"
          defaultValue="Disable webhook?"
        />
      ),
      message: (
        <T
          keyName="webhook_disable_confirmation_message"
          defaultValue="This webhook will no longer receive events until re-enabled."
        />
      ),
      onConfirm() {
        performToggle(false);
      },
    });
  }

  return (
    <Tooltip
      title={
        data.enabled
          ? t('webhook_toggle_enabled', 'Enabled')
          : t('webhook_toggle_disabled', 'Disabled')
      }
    >
      <Switch
        size="small"
        checked={data.enabled}
        onChange={handleToggle}
        disabled={toggleWebhook.isLoading}
        data-cy="webhook-item-toggle"
      />
    </Tooltip>
  );
};
