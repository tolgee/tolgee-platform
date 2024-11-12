import { useState } from 'react';
import { T, useTranslate } from '@tolgee/react';
import { Plus } from '@untitled-ui/icons-react';
import { Box, Button, Typography } from '@mui/material';

import { BoxLoading } from 'tg.component/common/BoxLoading';
import { PaginatedHateoasList } from 'tg.component/common/list/PaginatedHateoasList';
import { PaidFeatureBanner } from 'tg.ee/common/PaidFeatureBanner';
import { useEnabledFeatures } from 'tg.globalContext/helpers';
import { useProject } from 'tg.hooks/useProject';
import { useProjectPermissions } from 'tg.hooks/useProjectPermissions';
import { useApiQuery } from 'tg.service/http/useQueryApi';

import { WebhookEditDialog } from './WebhookEditDialog';
import { WebhookItem } from './WebhookItem';

export const WebhookList = () => {
  const project = useProject();
  const [page, setPage] = useState(0);
  const [formOpen, setFormOpen] = useState(false);
  const { t } = useTranslate();

  const webhooksLoadable = useApiQuery({
    url: '/v2/projects/{projectId}/webhook-configs',
    method: 'get',
    path: { projectId: project.id },
    query: { page },
  });

  const features = useEnabledFeatures();
  const { satisfiesPermission } = useProjectPermissions();

  const canAdd = satisfiesPermission('webhooks.manage');

  const isEnabled = features.isEnabled('WEBHOOKS');

  const itemsCount =
    webhooksLoadable.data?._embedded?.webhookConfigs?.length ?? 0;

  if (webhooksLoadable.isLoading) {
    return (
      <Box mt={6}>
        <BoxLoading />
      </Box>
    );
  }

  return (
    <Box mt={4}>
      <Box
        display="flex"
        justifyContent="space-between"
        alignItems="center"
        flexWrap="wrap"
        gap={2}
        pb={1.5}
      >
        <Typography component="h5" variant="h5" data-cy="webhooks-subtitle">
          {t('webhooks_subtitle')}
        </Typography>
        <Button
          variant="contained"
          color="primary"
          onClick={() => setFormOpen(true)}
          disabled={!canAdd || !isEnabled}
          startIcon={<Plus width={19} height={19} />}
          data-cy="webhooks-add-item-button"
        >
          {t('webhooks_add_button')}
        </Button>
      </Box>

      <PaginatedHateoasList
        loadable={webhooksLoadable}
        renderItem={(item) => <WebhookItem data={item} />}
        onPageChange={(val) => setPage(val)}
        emptyPlaceholder={
          <Box display="flex" justifyContent="center">
            <T keyName="global_empty_list_message" />
          </Box>
        }
      />

      {!isEnabled && (
        <Box mt={6}>
          <PaidFeatureBanner
            customTitle={
              itemsCount > 0 ? t('webhooks_over_limit_title') : undefined
            }
            customMessage={t('webhooks_not_enabled_message')}
          />
        </Box>
      )}

      {formOpen && <WebhookEditDialog onClose={() => setFormOpen(false)} />}
    </Box>
  );
};
