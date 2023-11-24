import { FormControl, InputLabel, MenuItem, Select } from '@mui/material';
import { Field } from 'formik';
import { components } from 'tg.service/apiSchema.generated';

import { useTranslate } from '@tolgee/react';

type ContentStorageModel = components['schemas']['ContentStorageModel'];

type Props = {
  name: string;
  items: ContentStorageModel[];
  label: string;
};

export const CdStorageSelector = ({ name, items, label }: Props) => {
  const { t } = useTranslate();
  return (
    <Field name={name}>
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
