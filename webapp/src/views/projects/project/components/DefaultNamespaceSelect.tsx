import React, { FC, ReactNode } from 'react';
import { Box, MenuItem } from '@mui/material';

import { Select } from 'tg.component/common/form/fields/Select';
import { components } from 'tg.service/apiSchema.generated';
import { FieldLabel } from 'tg.component/FormField';

const NamespaceValue: FC<{
  namespace?: Partial<components['schemas']['NamespaceModel']>;
}> = (props) => {
  return (
    <Box display="inline-flex" justifyContent="center" justifyItems="center">
      {props.namespace?.name ? props.namespace.name : '<none>'}
    </Box>
  );
};

export const DefaultNamespaceSelect: FC<{
  namespaces: Partial<components['schemas']['NamespaceModel']>[];
  label?: ReactNode;
  name: string;
  valueKey?: keyof components['schemas']['NamespaceModel'];
}> = (props) => {
  const namespacesWithNone = props.namespaces.map((namespace) => {
    if (!namespace.name) {
      namespace.name = '<none>';
    }
    return namespace;
  });

  return (
    <Box>
      <FieldLabel>{props.label}</FieldLabel>
      <Select
        data-cy="base-namespace-select"
        sx={{ mt: 0 }}
        name={props.name}
        size="small"
        renderValue={(v) => {
          return (
            <NamespaceValue
              namespace={namespacesWithNone.find(
                (namespace) => namespace.id === v
              )}
            />
          );
        }}
      >
        {namespacesWithNone.map((namespace) => (
          <MenuItem key={namespace.id} value={namespace.id}>
            <NamespaceValue namespace={namespace} />
          </MenuItem>
        ))}
      </Select>
    </Box>
  );
};
