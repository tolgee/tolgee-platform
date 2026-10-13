import { useField } from 'formik';
import { Checkbox, ListItemText, MenuItem } from '@mui/material';
import { useTranslate } from '@tolgee/react';

import { Select } from 'tg.component/common/Select';
import { useProject } from 'tg.hooks/useProject';

type Props = {
  namespaces: string[] | undefined;
  className?: string;
};

export const NsSelector: React.FC<React.PropsWithChildren<Props>> = ({
  namespaces,
  className,
}) => {
  const { t } = useTranslate();
  const project = useProject();

  const [field, meta] = useField<string[]>('namespaces');

  if (!namespaces || !project.useNamespaces) {
    return null;
  }

  return (
    <div className={className}>
      <Select
        {...field}
        label={t('export_translations_namespaces_label')}
        error={meta.error}
        minHeight={false}
        shrinkable
        data-cy="export-namespace-selector"
        renderValue={(value) => {
          const values = value as string[];
          if (values.length === namespaces.length || values.length === 0) {
            return t('export_translations_namespaces_all');
          }
          return values.map((ns) => ns || t('namespace_default')).join(', ');
        }}
        displayEmpty={true}
        multiple
      >
        {namespaces.map((ns) => (
          <MenuItem
            data-cy="export-namespace-selector-item"
            data-cy-namespace={ns}
            key={ns}
            value={ns}
            dense
          >
            <Checkbox checked={field.value.includes(ns)} />
            <ListItemText primary={ns || t('namespace_default')} />
          </MenuItem>
        ))}
      </Select>
    </div>
  );
};
