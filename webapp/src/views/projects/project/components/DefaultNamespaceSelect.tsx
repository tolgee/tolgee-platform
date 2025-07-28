import { FC, ReactNode } from 'react';
import { Box, MenuItem, SxProps } from '@mui/material';

import { components } from 'tg.service/apiSchema.generated';
import { FieldLabel } from 'tg.component/FormField';
import { useTranslate } from '@tolgee/react';
import { Select } from 'tg.component/common/Select';

type NamespaceModel = components['schemas']['NamespaceModel'];

type NamespaceItem = {
  value: number | string;
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

type Props = {
  namespaces: Partial<NamespaceModel>[];
  label?: ReactNode;
  name: string;
  hidden: boolean;
  value: number | undefined;
  onChange: (nsId: number | undefined) => void;
  sx?: SxProps;
};

export const DefaultNamespaceSelect = (props: Props) => {
  const { t } = useTranslate();

  if (props.hidden) {
    return null;
  }

  const namespaces = props.namespaces.map(({ id, name }) => ({
    value: id ?? '',
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
        value={props.value ?? ''}
        onChange={(e) =>
          props.onChange(
            (e.target.value || undefined) as unknown as number | undefined
          )
        }
        renderValue={(v) => {
          return (
            <NamespaceValue
              namespace={namespaces.find((namespace) => namespace.value === v)}
            />
          );
        }}
      >
        {namespaces.map((namespace) => (
          <MenuItem key={namespace.value} value={namespace.value}>
            <NamespaceValue namespace={namespace} />
          </MenuItem>
        ))}
      </Select>
    </Box>
  );
};
