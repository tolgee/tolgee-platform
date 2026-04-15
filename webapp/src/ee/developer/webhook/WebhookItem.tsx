import { useState } from 'react';
import { T, useTranslate } from '@tolgee/react';
import {
  Box,
  Button,
  Dialog,
  DialogActions,
  DialogContent,
  DialogTitle,
  styled,
  Switch,
  Tooltip,
  useTheme,
} from '@mui/material';
import { AlertCircle } from '@untitled-ui/icons-react';

import { components } from 'tg.service/apiSchema.generated';
import LoadingButton from 'tg.component/common/form/LoadingButton';
import { useApiMutation } from 'tg.service/http/useQueryApi';
import { useProject } from 'tg.hooks/useProject';
import { useMessage } from 'tg.hooks/useSuccessMessage';

import { CopyUrlItem } from 'tg.views/projects/developer/CopyUrlItem';
import { WebhookEditDialog } from './WebhookEditDialog';
import { useDateFormatter } from 'tg.hooks/useLocale';
import { confirmation } from 'tg.hooks/confirmation';

type WebhookConfigModel = components['schemas']['WebhookConfigModel'];

const StyledContainer = styled('div')`
  display: flex;
  padding: 8px 16px;
  align-items: center;
  justify-content: space-between;
  flex-wrap: wrap;
  gap: 10px;
  & + & {
    border-top: 1px solid ${({ theme }) => theme.palette.divider};
  }
`;

const StyledTime = styled('div')`
  text-decoration: underline;
  text-decoration-style: dashed;
  text-underline-offset: 4px;
  text-decoration-thickness: 1px;
`;

type Props = {
  data: WebhookConfigModel;
};

export const WebhookItem = ({ data }: Props) => {
  const project = useProject();
  const messaging = useMessage();
  const theme = useTheme();
  const { t } = useTranslate();
  const formatDate = useDateFormatter();
  const [formOpen, setFormOpen] = useState(false);
  const [keyOpen, setKeyOpen] = useState(false);

  const testLoadable = useApiMutation({
    url: '/v2/projects/{projectId}/webhook-configs/{id}/test',
    method: 'post',
  });

  const toggleWebhook = useApiMutation({
    url: '/v2/projects/{projectId}/webhook-configs/{id}',
    method: 'put',
    invalidatePrefix: '/v2/projects/{projectId}/webhook-configs',
  });

  function handleTest() {
    testLoadable.mutate(
      {
        path: { projectId: project.id, id: data.id },
      },
      {
        onSuccess(data) {
          if (data.success) {
            messaging.success(<T keyName="webhook_test_success" />);
          } else {
            messaging.error(<T keyName="webhook_test_fail" />);
          }
        },
      }
    );
  }

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
    if (data.enabled) {
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
    } else {
      performToggle(true);
    }
  }

  const isAutoDisabled = !data.enabled && data.firstFailed;

  const toggleTooltip = isAutoDisabled
    ? t(
        'webhook_auto_disabled_hint',
        'Automatically disabled due to persistent failures'
      )
    : data.enabled
    ? t('webhook_toggle_disable_hint', 'Disable webhook')
    : t('webhook_toggle_enable_hint', 'Enable webhook');

  return (
    <StyledContainer
      data-cy="webhooks-list-item"
      data-cy-url={data.url}
      sx={{ opacity: data.enabled ? 1 : 0.6 }}
    >
      <Box display="flex" gap={2} alignItems="center">
        <Tooltip title={toggleTooltip}>
          <Switch
            size="small"
            checked={data.enabled}
            onChange={handleToggle}
            disabled={toggleWebhook.isLoading}
            data-cy="webhook-item-toggle"
          />
        </Tooltip>
        <Box>{data.url}</Box>
        {Boolean(data.lastExecuted) && (
          <Tooltip title={t('webhooks_last_run_hint')}>
            <StyledTime>
              {formatDate(data.lastExecuted!, {
                timeStyle: 'short',
                dateStyle: 'short',
              })}
            </StyledTime>
          </Tooltip>
        )}
        {data.firstFailed && (
          <Tooltip title={t('webhooks_failing_hint')}>
            <Box color={theme.palette.error.main}>
              <AlertCircle width={18} height={18} />
            </Box>
          </Tooltip>
        )}
      </Box>
      <Box
        display="flex"
        gap={2}
        alignItems="center"
        justifyContent="end"
        flexGrow={1}
      >
        <Button
          size="small"
          onClick={() => setFormOpen(true)}
          data-cy="webhooks-item-edit"
        >
          {t('webhook_item_edit')}
        </Button>
        <Button
          size="small"
          onClick={() => setKeyOpen(true)}
          data-cy="webhooks-item-show-secret"
        >
          {t('webhook_item_show_secret')}
        </Button>
        <LoadingButton
          size="small"
          color="primary"
          loading={testLoadable.isLoading}
          onClick={handleTest}
          data-cy="webhooks-item-test"
        >
          {t('webhook_item_test')}
        </LoadingButton>
      </Box>
      {formOpen && (
        <WebhookEditDialog onClose={() => setFormOpen(false)} data={data} />
      )}
      {keyOpen && (
        <Dialog open={true} onClose={() => setKeyOpen(false)}>
          <DialogTitle>{t('webhook_secret_title')}</DialogTitle>
          <DialogContent
            sx={{
              width: '85vw',
              maxWidth: 600,
            }}
          >
            <Box mb={1}>{t('webhook_secret_description')}</Box>
            <CopyUrlItem value={data.webhookSecret || ''} maxWidth={550} />
          </DialogContent>
          <DialogActions>
            <Button onClick={() => setKeyOpen(false)}>
              {t('global_close_button')}
            </Button>
          </DialogActions>
        </Dialog>
      )}
    </StyledContainer>
  );
};
