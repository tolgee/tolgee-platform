import { DialogContent } from '@mui/material';
import { useTranslate } from '@tolgee/react';
import { Form, Formik, setNestedObjectValues } from 'formik';

import { TextField } from 'tg.component/common/form/fields/TextField';
import { components } from 'tg.service/apiSchema.generated';
import { Validation } from 'tg.constants/GlobalValidationSchema';
import { StorageFormActions } from './StorageFormActions';

type ContentStorageRequest = components['schemas']['ContentStorageRequest'];
type ContentStorageModel = components['schemas']['ContentStorageModel'];

type FormResult = {
  name: string;
  publicUrlPrefix: string;
  azureContentStorageConfig: ContentStorageRequest['azureContentStorageConfig'];
};

type Props = {
  onSubmit: (values: FormResult) => void;
  isSubmitting: boolean;
  onTest: (values: FormResult) => void;
  isTesting: boolean;
  onDelete?: () => void;
  isDeleting?: boolean;
  onClose: () => void;
  data?: ContentStorageModel;
};

export const StorageFormAzure = ({
  data,
  onSubmit,
  isSubmitting,
  onTest,
  isTesting,
  onDelete,
  onClose,
  isDeleting,
}: Props) => {
  const { t } = useTranslate();

  const isEdit = data?.azureContentStorageConfig;

  const keepAsIsPlaceholder = isEdit
    ? `<${t('storage_form_keep_as_is')}>`
    : undefined;
  const keepAsIsInputLabelProps = isEdit ? { shrink: true } : undefined;

  return (
    <Formik
      initialValues={{
        name: data?.name ?? '',
        publicUrlPrefix: data?.publicUrlPrefix ?? '',
        azureContentStorageConfig: {
          connectionString: '',
          containerName: data?.azureContentStorageConfig?.containerName ?? '',
        },
      }}
      onSubmit={(values) => {
        onSubmit(values);
      }}
      validationSchema={
        data
          ? Validation.STORAGE_FORM_AZURE_CREATE
          : Validation.STORAGE_FORM_AZURE_UPDATE
      }
    >
      {({ submitForm, values, validateForm, setTouched }) => (
        <Form>
          <DialogContent>
            <TextField
              size="small"
              variant="standard"
              name="name"
              label={t('storage_form_name')}
              data-cy="storage-form-name"
            />
            <TextField
              size="small"
              variant="standard"
              name="azureContentStorageConfig.connectionString"
              label="Connection string"
              placeholder={keepAsIsPlaceholder}
              InputLabelProps={keepAsIsInputLabelProps}
              data-cy="storage-form-azure-connection-string"
            />
            <TextField
              size="small"
              variant="standard"
              name="azureContentStorageConfig.containerName"
              label="Container name"
              data-cy="storage-form-azure-container-name"
            />
            <TextField
              size="small"
              variant="standard"
              name="publicUrlPrefix"
              label={t('storage_form_public_url_prefix')}
              data-cy="storage-form-public-url-prefix"
            />
          </DialogContent>
          <StorageFormActions
            onSubmit={submitForm}
            isSubmitting={isSubmitting}
            onTest={async () => {
              const errors = await validateForm();
              if (Object.keys(errors).length > 0) {
                setTouched(setNestedObjectValues(errors, true));
                return;
              }
              onTest(values);
            }}
            isTesting={isTesting}
            onDelete={isEdit ? onDelete : undefined}
            isDeleting={isDeleting}
            onClose={onClose}
          />
        </Form>
      )}
    </Formik>
  );
};
