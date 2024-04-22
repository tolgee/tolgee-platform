import { useState } from 'react';
import { T, useTranslate } from '@tolgee/react';
import { Box, Button, Chip, styled, Tooltip } from '@mui/material';

import LoadingButton from 'tg.component/common/form/LoadingButton';
import { useProject } from 'tg.hooks/useProject';
import { useProjectPermissions } from 'tg.hooks/useProjectPermissions';
import { components } from 'tg.service/apiSchema.generated';
import { useApiMutation } from 'tg.service/http/useQueryApi';
import { useMessage } from 'tg.hooks/useSuccessMessage';

import { CopyUrlItem } from '../CopyUrlItem';
import { CdDialog } from './CdDialog';
import { useDateFormatter } from 'tg.hooks/useLocale';

type ContentDeliveryConfigModel =
  components['schemas']['ContentDeliveryConfigModel'];

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
  data: ContentDeliveryConfigModel;
};

export const CdItem = ({ data }: Props) => {
  const { t } = useTranslate();
  const [formOpen, setFormOpen] = useState(false);
  const messaging = useMessage();
  const project = useProject();
  const formatDate = useDateFormatter();

  const { satisfiesPermission } = useProjectPermissions();

  const canPublish = satisfiesPermission('content-delivery.publish');
  const canEdit = satisfiesPermission('content-delivery.manage');

  const publishLoadable = useApiMutation({
    url: '/v2/projects/{projectId}/content-delivery-configs/{id}',
    method: 'post',
  });

  return (
    <StyledContainer
      data-cy="content-delivery-list-item"
      data-cy-name={data.name}
    >
      <Box display="flex" gap={2} alignItems="center">
        <div>{data.name}</div>
        {Boolean(data.lastPublished) && (
          <Tooltip title={t('content_delivery_last_publish_hint')}>
            <StyledTime>
              {formatDate(data.lastPublished!, {
                timeStyle: 'short',
                dateStyle: 'short',
              })}
            </StyledTime>
          </Tooltip>
        )}
        <Chip
          data-cy="content-delivery-item-type"
          size="small"
          label={
            data.autoPublish
              ? t('content_delivery_deployment_auto')
              : t('content_delivery_deployment_manual')
          }
        />
      </Box>
      <Box
        display="flex"
        gap={2}
        alignItems="center"
        justifyContent="end"
        flexGrow={1}
      >
        <CopyUrlItem value={data.publicUrl || ''} maxWidth={570} />
        {canEdit && (
          <Button
            size="small"
            onClick={() => setFormOpen(true)}
            data-cy="content-delivery-item-edit"
          >
            {t('content_delivery_item_edit')}
          </Button>
        )}
        <LoadingButton
          size="small"
          disabled={!canPublish}
          onClick={() =>
            publishLoadable.mutate(
              {
                path: { id: data.id, projectId: project.id },
              },
              {
                onSuccess() {
                  messaging.success(
                    <T keyName="content_delivery_publis_success" />
                  );
                },
              }
            )
          }
          color="primary"
          loading={publishLoadable.isLoading}
          data-cy="content-delivery-item-publish"
        >
          {t('content_delivery_item_publish')}
        </LoadingButton>
      </Box>
      {formOpen && <CdDialog onClose={() => setFormOpen(false)} data={data} />}
    </StyledContainer>
  );
};
