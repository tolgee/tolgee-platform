import { useField } from 'formik';
import {
  Checkbox,
  FormControl,
  FormHelperText,
  InputLabel,
  ListItemText,
  MenuItem,
  Select,
  SelectChangeEvent,
} from '@mui/material';
import { useTranslate } from '@tolgee/react';

import { StateType } from 'tg.constants/translationStates';
import { useProject } from 'tg.hooks/useProject';

type Props = {
  namespaces: string[] | undefined;
  className: string;
};

export const NsSelector: React.FC<Props> = ({ namespaces, className }) => {
  const { t } = useTranslate();
  const project = useProject();

  const [field, meta, helper] = useField('namespaces');

  if (!namespaces) {
    return null;
  }

  const onChange = (e: SelectChangeEvent<StateType[]>) => {
    helper.setValue(e.target.value);
  };

  if (!project.useNamespaces) {
    return null;
  }

  return (
    <FormControl className={className} error={!!meta.error} variant="standard">
      <InputLabel shrink={true}>
        {t('export_translations_namespaces_label')}
      </InputLabel>
      <Select
        {...field}
        onChange={onChange}
        variant="standard"
        data-cy="export-namespace-selector"
        renderValue={(values: StateType[]) => {
          if (values.length === namespaces.length || values.length === 0) {
            return t('export_translations_namespaces_all');
          }
          return values.map((ns) => ns || t('namespace_default')).join(', ');
        }}
        displayEmpty={true}
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
};
