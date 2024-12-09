import { useState } from 'react';
import { T, useTranslate } from '@tolgee/react';
import { Box, Button, Chip, styled } from '@mui/material';

import LoadingButton from 'tg.component/common/form/LoadingButton';
import { useProject } from 'tg.hooks/useProject';
import { useProjectPermissions } from 'tg.hooks/useProjectPermissions';
import { components } from 'tg.service/apiSchema.generated';
import { useApiMutation } from 'tg.service/http/useQueryApi';
import { useMessage } from 'tg.hooks/useSuccessMessage';

import { CdDialog } from './CdDialog';
import { CdFilesRow } from './CdFilesRow';
import { useDateFormatter } from 'tg.hooks/useLocale';

type ContentDeliveryConfigModel =
  components['schemas']['ContentDeliveryConfigModel'];

const StyledContainer = styled('div')`
  display: grid;
  & + & {
    border-top: 1px solid ${({ theme }) => theme.palette.divider};
  }
  padding: 8px 16px;
`;

const StyledLastPublish = styled('div')`
  font-size: 14px;
  color: ${({ theme }) => theme.palette.text.secondary};
  @container (max-width: 700px) {
    display: none;
  }
`;

const StyledWrapper = styled('div')`
  display: grid;
  grid-template-columns: 1fr auto auto;
  align-items: center;
  flex-wrap: wrap;
  gap: 24px;
`;

const StyledName = styled('div')`
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
  font-weight: 700;
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
    invalidatePrefix: '/v2/projects/{projectId}/content-delivery-configs',
  });

  return (
    <StyledContainer
      data-cy="content-delivery-list-item"
      data-cy-name={data.name}
    >
      <StyledWrapper>
        <Box display="flex" gap={2} alignItems="center">
          <StyledName>{data.name}</StyledName>
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
        <StyledLastPublish>
          {data.lastPublished && (
            <>
              <T keyName="content_delivery_last_publish" />{' '}
              {formatDate(data.lastPublished!, {
                timeStyle: 'medium',
                dateStyle: 'short',
              })}
            </>
          )}
        </StyledLastPublish>
        <Box display="flex" gap={1} alignItems="center" justifyContent="end">
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
      </StyledWrapper>
      <CdFilesRow data={data} />
      {formOpen && <CdDialog onClose={() => setFormOpen(false)} data={data} />}
    </StyledContainer>
  );
};
