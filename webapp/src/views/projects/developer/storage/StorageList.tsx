import { useState } from 'react';
import { T, useTranslate } from '@tolgee/react';
import { Plus } from '@untitled-ui/icons-react';
import { Box, Button, Typography } from '@mui/material';

import { BoxLoading } from 'tg.component/common/BoxLoading';
import { PaginatedHateoasList } from 'tg.component/common/list/PaginatedHateoasList';
import { PaidFeatureBanner } from 'tg.ee/common/PaidFeatureBanner';
import { useEnabledFeatures } from 'tg.globalContext/helpers';
import { useProject } from 'tg.hooks/useProject';
import { useApiQuery } from 'tg.service/http/useQueryApi';
import { useGlobalContext } from 'tg.globalContext/GlobalContext';

import { CdNotConfiguredAlert } from '../CdNotConfiguredAlert';
import { StorageEditDialog } from './StorageEditDialog';
import { StorageItem } from './StorageItem';
import { useProjectPermissions } from 'tg.hooks/useProjectPermissions';

export const StorageList = () => {
  const project = useProject();
  const [page, setPage] = useState(0);
  const [formOpen, setFormOpen] = useState(false);
  const { t } = useTranslate();

  const storagesLoadable = useApiQuery({
    url: '/v2/projects/{projectId}/content-storages',
    method: 'get',
    path: { projectId: project.id },
    query: { page },
  });

  const { satisfiesPermission } = useProjectPermissions();

  const canManage = satisfiesPermission('content-delivery.manage');

  const serverConfiguration = useGlobalContext(
    (c) => c.initialData.serverConfiguration
  );
  const contentDeliveryConfigured =
    serverConfiguration.contentDeliveryConfigured;

  const features = useEnabledFeatures();

  if (!contentDeliveryConfigured) {
    return <CdNotConfiguredAlert />;
  }

  const itemsCount =
    storagesLoadable.data?._embedded?.contentStorages?.length ?? 0;

  const isEnabled = features.isEnabled('PROJECT_LEVEL_CONTENT_STORAGES');

  if (storagesLoadable.isLoading) {
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
        pb={4}
        flexWrap="wrap"
        gap={2}
      >
        <Typography component="h5" variant="h5" data-cy="storage-subtitle">
          {t('storage_subtitle')}
        </Typography>
        <Button
          variant="contained"
          color="primary"
          onClick={() => setFormOpen(true)}
          startIcon={<Plus width={19} height={19} />}
          data-cy="storage-add-item-button"
          disabled={!canManage || !isEnabled}
        >
          {t('storage_add_item')}
        </Button>
      </Box>
      <PaginatedHateoasList
        loadable={storagesLoadable}
        renderItem={(item) => <StorageItem data={item} />}
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
              itemsCount === 0 ? undefined : t('storage_over_limit_title')
            }
            customMessage={t('storage_not_enabled_message')}
          />
        </Box>
      )}

      {formOpen && <StorageEditDialog onClose={() => setFormOpen(false)} />}
    </Box>
  );
};
