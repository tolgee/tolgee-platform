import { FC, ReactNode } from 'react';
import { Box, MenuItem } from '@mui/material';

import { Select } from 'tg.component/common/form/fields/Select';
import { components } from 'tg.service/apiSchema.generated';
import { FieldLabel } from 'tg.component/FormField';
import { useTranslate } from '@tolgee/react';

type NamespaceModel = components['schemas']['NamespaceModel'];

type NamespaceItem = {
  value: number | '';
  label: string;
};

const NamespaceValue: FC<{
  namespace?: NamespaceItem;
}> = (props) => {
  const { t } = useTranslate();
  return (
    <Box
      data-cy="namespace-value"
      display="inline-flex"
      justifyContent="center"
      justifyItems="center"
    >
      {props.namespace?.label ?? t('namespace_default')}
    </Box>
  );
};

export const DefaultNamespaceSelect: FC<{
  namespaces: Partial<NamespaceModel>[];
  label?: ReactNode;
  name: string;
  valueKey?: keyof NamespaceModel;
}> = (props) => {
  const { t } = useTranslate();
  const namespaces = props.namespaces.map(({ id, name }) => ({
    value: id ?? ('' as const),
    label: name ?? t('namespace_default'),
  }));

  return (
    <Box>
      <FieldLabel>{props.label}</FieldLabel>
      <Select
        data-cy="default-namespace-select"
        sx={{ mt: 0 }}
        name={props.name}
        size="small"
        minHeight={false}
        displayEmpty={true}
        renderValue={(v) => {
          return (
            <NamespaceValue
              namespace={namespaces.find((namespace) => namespace.value === v)}
            />
          );
        }}
      >
        {namespaces.map((namespace) => (
          <MenuItem key={namespace.value} value={namespace.value as any}>
            <NamespaceValue namespace={namespace} />
          </MenuItem>
        ))}
      </Select>
    </Box>
  );
};
