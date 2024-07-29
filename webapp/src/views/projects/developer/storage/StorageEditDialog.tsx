import { useState } from 'react';
import { T, useTranslate } from '@tolgee/react';
import {
  Box,
  Button,
  ButtonGroup,
  Dialog,
  DialogTitle,
  styled,
} from '@mui/material';

import { useProject } from 'tg.hooks/useProject';
import { useApiMutation } from 'tg.service/http/useQueryApi';
import { components } from 'tg.service/apiSchema.generated';
import { confirmation } from 'tg.hooks/confirmation';
import { useErrorTranslation } from 'tg.translationTools/useErrorTranslation';
import { useMessage } from 'tg.hooks/useSuccessMessage';

import { StorageFormAzure } from './StorageFormAzure';
import { StorageFormS3 } from './StorageFormS3';

type ContentStorageRequest = components['schemas']['ContentStorageRequest'];
type ContentStorageModel = components['schemas']['ContentStorageModel'];
type StorageTestResult = components['schemas']['StorageTestResult'];
type BucketType = 's3' | 'azure';

const StyledDialogTitle = styled(DialogTitle)`
  width: 85vw;
  max-width: 700px;
`;

type Props = {
  onClose: () => void;
  data?: ContentStorageModel;
};

export const StorageEditDialog = ({ onClose, data }: Props) => {
  const { t } = useTranslate();
  const project = useProject();
  const translateError = useErrorTranslation();
  const messaging = useMessage();

  const [bucketType, setBucketType] = useState<BucketType>(
    data?.azureContentStorageConfig ? 'azure' : 's3'
  );

  const createStorage = useApiMutation({
    url: '/v2/projects/{projectId}/content-storages',
    method: 'post',
    invalidatePrefix: '/v2/projects/{projectId}/content-storages',
  });

  const updateStorage = useApiMutation({
    url: '/v2/projects/{projectId}/content-storages/{contentStorageId}',
    method: 'put',
    invalidatePrefix: '/v2/projects/{projectId}/content-storages',
  });

  const deleteItem = useApiMutation({
    url: '/v2/projects/{projectId}/content-storages/{contentStorageId}',
    method: 'delete',
    invalidatePrefix: '/v2/projects/{projectId}/content-storages',
  });

  const testNew = useApiMutation({
    url: '/v2/projects/{projectId}/content-storages/test',
    method: 'post',
  });

  const testExisting = useApiMutation({
    url: '/v2/projects/{projectId}/content-storages/{id}/test',
    method: 'post',
  });

  function handleDelete() {
    confirmation({
      title: <T keyName="storage_item_delete_dialog_title" />,
      onConfirm() {
        deleteItem.mutate(
          {
            path: { projectId: project.id, contentStorageId: data!.id },
          },
          {
            onSuccess() {
              onClose();
              messaging.success(<T keyName="storage_delete_success" />);
            },
          }
        );
      },
    });
  }

  function handleSumbit(values: ContentStorageRequest) {
    if (data) {
      updateStorage.mutate(
        {
          path: { projectId: project.id, contentStorageId: data.id },
          content: {
            'application/json': values,
          },
        },
        {
          onSuccess() {
            onClose();
            messaging.success(<T keyName="storage_update_success" />);
          },
        }
      );
    } else {
      createStorage.mutate(
        {
          path: { projectId: project.id },
          content: { 'application/json': values },
        },
        {
          onSuccess() {
            onClose();
            messaging.success(<T keyName="storage_create_success" />);
          },
        }
      );
    }
  }

  function printTestResult(data: StorageTestResult) {
    if (data.success) {
      messaging.success(<T keyName="storage_form_test_success" />);
    } else if (data.message) {
      messaging.error(translateError(data.message));
    }
  }

  function handleTest(values: ContentStorageRequest) {
    if (data) {
      testExisting.mutate(
        {
          path: { projectId: project.id, id: data.id },
          content: {
            'application/json': values,
          },
        },
        {
          onSuccess: printTestResult,
        }
      );
    } else {
      testNew.mutate(
        {
          path: { projectId: project.id },
          content: { 'application/json': values },
        },
        {
          onSuccess: printTestResult,
        }
      );
    }
  }

  const isSubmitting = createStorage.isLoading || updateStorage.isLoading;
  const isTesting = testNew.isLoading || testExisting.isLoading;

  const formProps = {
    data,
    onSubmit: handleSumbit,
    isSubmitting,
    onDelete: handleDelete,
    isDeleting: deleteItem.isLoading,
    onTest: handleTest,
    isTesting,
    onClose,
  };

  return (
    <Dialog open onClose={onClose} maxWidth="sm">
      <StyledDialogTitle>
        {data ? t('storage_update_title') : t('storage_create_title')}
      </StyledDialogTitle>
      <Box px={3}>
        <ButtonGroup size="small">
          <Button
            color={bucketType === 's3' ? 'primary' : 'default'}
            onClick={() => setBucketType('s3')}
            data-cy="storage-form-type-s3"
          >
            {t('storage_form_type_s3')}
          </Button>
          <Button
            color={bucketType === 'azure' ? 'primary' : 'default'}
            onClick={() => setBucketType('azure')}
            data-cy="storage-form-type-azure"
          >
            {t('storage_form_type_azure')}
          </Button>
        </ButtonGroup>
      </Box>
      {bucketType === 's3' ? (
        <StorageFormS3 {...formProps} />
      ) : (
        <StorageFormAzure {...formProps} />
      )}
    </Dialog>
  );
};
