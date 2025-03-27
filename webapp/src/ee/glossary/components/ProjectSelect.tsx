import { components } from 'tg.service/apiSchema.generated';
import React, { ComponentProps, ReactNode } from 'react';
import Box from '@mui/material/Box';
import { FieldLabel } from 'tg.component/FormField';
import { Select } from 'tg.component/common/Select';
import { MenuItem } from '@mui/material';
import { useFormikContext } from 'formik';
import { useTranslate } from '@tolgee/react';

type SimpleProjectModel = components['schemas']['SimpleProjectModel'];

type Props = {
  label?: ReactNode;
  name: string;
  valueKey?: React.KeyOf<SimpleProjectModel>;
  available: SimpleProjectModel[];
} & Omit<ComponentProps<typeof Box>, 'children'>;

export const ProjectSelect: React.VFC<Props> = ({
  label,
  name,
  valueKey = 'id',
  available,
  ...boxProps
}) => {
  const context = useFormikContext();
  const { t } = useTranslate();
  const value = context.getFieldProps(name).value as React.Key[];

  return (
    <Box {...boxProps}>
      <FieldLabel>{label}</FieldLabel>
      <Select
        data-cy="project-select"
        size="small"
        multiple
        value={value}
        onChange={(e) => {
          context.setFieldValue(name, e.target.value);
        }}
        SelectDisplayProps={{
          style: {
            contain: 'inline-size',
          },
        }}
        displayEmpty
        renderValue={(v) => {
          const value = v as React.Key[];
          if (value.length === 0) {
            return t('project_select_empty_value');
          }
          return value
            .map((k) => available.find((p) => p[valueKey] === k))
            .map((p) => p?.name)
            .join(', ');
        }}
      >
        {available.map((p) => (
          <MenuItem key={p[valueKey]} value={p[valueKey]}>
            {p.name}
            {/*  TODO: next project view for this select*/}
          </MenuItem>
        ))}
      </Select>
    </Box>
  );
};
