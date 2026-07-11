import { DialogContent } from '@mui/material';
import { useTranslate } from '@tolgee/react';
import { Form, Formik, setNestedObjectValues } from 'formik';

import { TextField } from 'tg.component/common/form/fields/TextField';
import { components } from 'tg.service/apiSchema.generated';
import { Validation } from 'tg.constants/GlobalValidationSchema';
import { LabelHint } from 'tg.component/common/LabelHint';
import { StorageFormActions } from './StorageFormActions';

type ContentStorageRequest = components['schemas']['ContentStorageRequest'];
type ContentStorageModel = components['schemas']['ContentStorageModel'];

type FormResult = {
  name: string;
  publicUrlPrefix: string;
  s3ContentStorageConfig: ContentStorageRequest['s3ContentStorageConfig'];
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

export const StorageFormS3 = ({
  data,
  onSubmit,
  isSubmitting,
  onTest,
  isTesting,
  onDelete,
  isDeleting,
  onClose,
}: Props) => {
  const { t } = useTranslate();

  const isEdit = data?.id !== undefined;
  const keepAsIsPlaceholder = isEdit
    ? `<${t('storage_form_keep_as_is')}>`
    : undefined;
  const keepAsIsInputLabelProps = isEdit ? { shrink: true } : undefined;

  return (
    <Formik
      initialValues={{
        name: data?.name ?? '',
        publicUrlPrefix: data?.publicUrlPrefix ?? '',
        s3ContentStorageConfig: {
          bucketName: data?.s3ContentStorageConfig?.bucketName ?? '',
          accessKey: '',
          secretKey: '',
          path: data?.s3ContentStorageConfig?.path ?? '',
          endpoint: data?.s3ContentStorageConfig?.endpoint ?? '',
          signingRegion: data?.s3ContentStorageConfig?.signingRegion ?? '',
        },
      }}
      onSubmit={(values) => {
        onSubmit(values);
      }}
      validationSchema={
        data
          ? Validation.STORAGE_FORM_S3_UPDATE
          : Validation.STORAGE_FORM_S3_CREATE
      }
    >
      {({ submitForm, validateForm, values, setTouched }) => {
        return (
          <Form>
            <DialogContent>
              <TextField
                size="small"
                name="name"
                label={t('storage_form_name')}
                data-cy="storage-form-name"
              />
              <TextField
                size="small"
                name="s3ContentStorageConfig.bucketName"
                label="Bucket name"
                data-cy="storage-form-s3-bucket-name"
              />
              <TextField
                size="small"
                name="s3ContentStorageConfig.path"
                label={
                  <LabelHint
                    title={t(
                      'storage_form_s3_path_hint',
                      'Optional subdirectory inside the bucket, e.g. "i18n". Leave empty to use the bucket root.'
                    )}
                  >
                    Path prefix
                  </LabelHint>
                }
                data-cy="storage-form-s3-path"
              />
              <TextField
                size="small"
                name="s3ContentStorageConfig.accessKey"
                label={
                  <LabelHint
                    title={t(
                      'storage_form_s3_access_key_hint',
                      'Requires "s3:PutObject", "s3:GetObject", "s3:DeleteObject" (and "s3:ListBucket" if "Prune before publish" is enabled) on the bucket. For Cloudflare R2: use an API token with "Object Read & Write" permission.'
                    )}
                  >
                    Access key
                  </LabelHint>
                }
                placeholder={keepAsIsPlaceholder}
                InputLabelProps={keepAsIsInputLabelProps}
                data-cy="storage-form-s3-access-key"
              />
              <TextField
                size="small"
                name="s3ContentStorageConfig.secretKey"
                label="Secret key"
                placeholder={keepAsIsPlaceholder}
                InputLabelProps={keepAsIsInputLabelProps}
                data-cy="storage-form-s3-secret-key"
              />
              <TextField
                size="small"
                name="s3ContentStorageConfig.endpoint"
                label={
                  <LabelHint
                    title={t(
                      'storage_form_s3_endpoint_hint',
                      'e.g. "{awsExample}", "{r2Example}"',
                      {
                        awsExample: 'https://s3.eu-central-1.amazonaws.com',
                        r2Example:
                          'https://<account-id>.r2.cloudflarestorage.com',
                      }
                    )}
                  >
                    Endpoint
                  </LabelHint>
                }
                data-cy="storage-form-s3-endpoint"
              />
              <TextField
                size="small"
                name="s3ContentStorageConfig.signingRegion"
                label={
                  <LabelHint
                    title={t(
                      'storage_form_s3_signing_region_hint',
                      'e.g. "eu-central-1", "auto" for Cloudflare'
                    )}
                  >
                    Signing region
                  </LabelHint>
                }
                data-cy="storage-form-s3-signing-region"
              />
              <TextField
                size="small"
                name="publicUrlPrefix"
                label={
                  <LabelHint
                    title={t(
                      'storage_form_s3_public_url_prefix_hint',
                      'e.g. "{awsExample}", "{r2Example}"',
                      {
                        awsExample:
                          'https://<bucket-name>.s3.eu-central-1.amazonaws.com',
                        r2Example:
                          'https://<account-id>.r2.cloudflarestorage.com/<bucket-name>',
                      }
                    )}
                  >
                    {t('storage_form_public_url_prefix')}
                  </LabelHint>
                }
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
        );
      }}
    </Formik>
  );
};
