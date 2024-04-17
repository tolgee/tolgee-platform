import { FormControl, InputLabel, MenuItem, Select } from '@mui/material';
import { Field, useField, useFormik, useFormikContext } from 'formik';
import { components } from 'tg.service/apiSchema.generated';

import { useTranslate } from '@tolgee/react';
import { FC } from 'react';
import { TextField } from 'tg.component/common/form/fields/TextField';
import { CdValues } from './getCdEditInitialValues';

type ContentStorageModel = components['schemas']['ContentStorageModel'];

type Props = {
  items: ContentStorageModel[];
};

export const CdStorageSelector = ({ items }: Props) => {
  const { t } = useTranslate();

  return (
    <>
      <StorageField items={items} />
      <CustomSlug />
    </>
  );
};

export const CustomSlug = () => {
  const { t } = useTranslate();

  const field = useFormikContext<CdValues>();
  const storageId = field.values.contentStorageId;

  if (!storageId) {
    return null;
  }

  return (
    <TextField
      name="slug"
      label={t('content_delivery_form_custom_slug_label')}
      helperText={t('content_delivery_form_custom_slug_helper_text')}
      variant="standard"
      data-cy="content-delivery-form-custom-sliug"
    />
  );
};

export const StorageField: FC<{
  items: components['schemas']['ContentStorageModel'][];
}> = ({ items }) => {
  const { t } = useTranslate();

  const label = t('content_delivery_form_storage');

  return (
    <Field name="contentStorageId">
      {({ field }) => (
        <FormControl variant="standard">
          <InputLabel shrink>{label}</InputLabel>
          <Select
            {...field}
            variant="standard"
            label={label}
            displayEmpty
            data-cy="content-delivery-storage-selector"
          >
            <MenuItem value={undefined}>{t('storage_item_default')}</MenuItem>
            {items.map((item) => (
              <MenuItem
                key={item.id}
                value={item.id}
                data-cy="content-delivery-storage-selector-item"
              >
                {item.name}
              </MenuItem>
            ))}
          </Select>
        </FormControl>
      )}
    </Field>
  );
};
