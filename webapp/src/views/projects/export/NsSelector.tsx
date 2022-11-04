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
import { components } from 'tg.service/apiSchema.generated';

type NamespaceModel = components['schemas']['NamespaceModel'];

type Props = {
  namespaces: NamespaceModel[] | undefined;
  className: string;
};

export const NsSelector: React.FC<Props> = ({ namespaces, className }) => {
  const t = useTranslate();

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
                return values.join(', ');
              }}
              multiple
            >
              {namespaces?.map((ns) => (
                <MenuItem
                  key={ns.id}
                  value={ns.name}
                  data-cy="export-namespace-selector-item"
                >
                  <Checkbox checked={field.value.includes(ns.name)} />
                  <ListItemText primary={ns.name || t('namespace_default')} />
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
