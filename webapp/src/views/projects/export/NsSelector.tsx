import { Field } from 'formik';
import {
  MenuItem,
  Select,
  Checkbox,
  ListItemText,
  FormControl,
  InputLabel,
  FormHelperText,
} from '@mui/material';
import { useTranslate } from '@tolgee/react';

import { StateType } from 'tg.constants/translationStates';

type Props = {
  namespaces: string[] | undefined;
  className: string;
};

export const NsSelector: React.FC<Props> = ({ namespaces, className }) => {
  const { t } = useTranslate();

  if (!namespaces) {
    return null;
  }

  return (
    <Field name="namespaces">
      {({ field, meta }) => {
        return (
          <FormControl
            className={className}
            error={meta.error}
            variant="standard"
          >
            <InputLabel>{t('export_translations_namespaces_label')}</InputLabel>
            <Select
              {...field}
              variant="standard"
              data-cy="export-namespace-selector"
              renderValue={(values: StateType[]) => {
                if (values.length === namespaces.length) {
                  return t('export_translations_namespaces_all');
                }
                return values
                  .map((ns) => ns || t('namespace_default'))
                  .join(', ');
              }}
              multiple
            >
              {namespaces?.map((ns) => (
                <MenuItem
                  data-cy="export-namespace-selector-item"
                  key={ns}
                  value={ns}
                  dense
                >
                  <Checkbox checked={field.value.includes(ns)} />
                  <ListItemText primary={ns || t('namespace_default')} />
                </MenuItem>
              ))}
            </Select>
            <FormHelperText>{meta.error}</FormHelperText>
          </FormControl>
        );
      }}
    </Field>
  );
};
