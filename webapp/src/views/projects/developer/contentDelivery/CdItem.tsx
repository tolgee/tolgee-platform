import { useState } from 'react';
import { T, useTranslate } from '@tolgee/react';
import { Box, Button, Chip, styled } from '@mui/material';

import LoadingButton from 'tg.component/common/form/LoadingButton';
import { useProject } from 'tg.hooks/useProject';
import { useProjectPermissions } from 'tg.hooks/useProjectPermissions';
import { components } from 'tg.service/apiSchema.generated';
import { useApiMutation } from 'tg.service/http/useQueryApi';
import { useMessage } from 'tg.hooks/useSuccessMessage';

import { CopyUrlItem } from '../CopyUrlItem';
import { CdDialog } from './CdDialog';
import { useDateFormatter } from 'tg.hooks/useLocale';
import { CdFileLink } from './CdFileLink';

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

const StyledLastPublish = styled('div')`
  display: flex;
  padding: 8px 16px;
  box-shadow: inset 0px 11px 5px -12px rgba(0, 0, 0, 0.5);
  background: ${({ theme }) => theme.palette.tokens.background.floating};
  gap: 12px;
  font-size: 14px;
  justify-self: stretch;
`;

const StyledButton = styled('span')`
  color: ${({ theme }) => theme.palette.primary.main};
  cursor: pointer;
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

  const [showAllFiles, setShowAllFiles] = useState(false);

  const publishLoadable = useApiMutation({
    url: '/v2/projects/{projectId}/content-delivery-configs/{id}',
    method: 'post',
    invalidatePrefix: '/v2/projects/{projectId}/content-delivery-configs',
  });

  const getFileUrl = (file: string) => {
    return data.publicUrl + '/' + file;
  };

  return (
    <Box display="grid">
      <StyledContainer
        data-cy="content-delivery-list-item"
        data-cy-name={data.name}
      >
        <Box display="flex" gap={2} alignItems="center">
          <div>{data.name}</div>
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
      </StyledContainer>
      {data.lastPublished && (
        <StyledLastPublish>
          <Box>
            <T keyName="content_delivery_last_publish" />{' '}
            {formatDate(data.lastPublished!, {
              timeStyle: 'medium',
              dateStyle: 'short',
            })}
          </Box>
          {showAllFiles ? (
            <Box display="grid">
              {data.files.map((file) => (
                <CdFileLink key={file} link={getFileUrl(file)} file={file} />
              ))}
              <StyledButton
                role="button"
                onClick={() => setShowAllFiles(false)}
              >
                <T keyName="content_delivery_show_less_files" />
              </StyledButton>
            </Box>
          ) : (
            <Box display="flex" justifyContent="space-between" flexGrow="1">
              <CdFileLink
                link={getFileUrl(data.files[0])}
                file={data.files[0]}
              />
              {data.files.length > 1 && (
                <StyledButton
                  role="button"
                  onClick={() => setShowAllFiles(true)}
                >
                  <T keyName="content_delivery_show_all_files" />
                </StyledButton>
              )}
            </Box>
          )}
        </StyledLastPublish>
      )}
      {formOpen && <CdDialog onClose={() => setFormOpen(false)} data={data} />}
    </Box>
  );
};
