import {
  Autocomplete,
  ListItem,
  styled,
  TextField,
  useTheme,
} from '@mui/material';
import { useTranslate } from '@tolgee/react';
import { useMemo } from 'react';
import { getTextWidth } from 'tg.fixtures/getTextWidth';

import { BatchActions } from './types';

const StyledSeparator = styled('div')`
  width: 100%;
  border: 1px solid ${({ theme }) => theme.palette.divider};
  border-width: 1px 0px 0px 0px;
`;

type Props = {
  value: BatchActions | undefined;
  onChange: (value: BatchActions | undefined) => void;
};

export const BatchSelect = ({ value, onChange }: Props) => {
  const theme = useTheme();
  const { t } = useTranslate();

  const options: { id: BatchActions; label: string; divider?: boolean }[] = [
    { id: 'machine_translate', label: t('batch_operations_machine_translate') },
    { id: 'pre_translate', label: t('batch_operations_pre_translate') },
    {
      id: 'mark_as_translated',
      label: t('batch_operations_mark_as_translated'),
    },
    { id: 'mark_as_reviewed', label: t('batch_operations_mark_as_reviewed') },
    { id: 'copy_translations', label: t('batch_operations_copy_translations') },
    {
      id: 'clear_translations',
      label: t('batch_operation_clear_translations'),
    },
    { id: 'add_tags', label: t('batch_operations_add_tags'), divider: true },
    { id: 'remove_tags', label: t('batch_operations_remove_tags') },
    { id: 'change_namespace', label: t('batch_operations_change_namespace') },
    { id: 'delete', label: t('batch_operations_delete') },
  ];

  const option = options.find((o) => o.id === value);

  const width = useMemo(() => {
    if (option?.label) {
      return (
        getTextWidth(option.label, `400 16px ${theme.typography.fontFamily}`) +
        85
      );
    }
    return 250;
  }, [option?.label]);

  const normalizedWidth = Math.min(Math.max(250, width), 350);

  return (
    <Autocomplete
      sx={{ width: normalizedWidth }}
      value={options.find((o) => o.id === value) || null}
      onChange={(_, value) => {
        onChange(value?.id);
      }}
      renderOption={(props, o) => (
        <>
          {o.divider && <StyledSeparator />}
          <ListItem {...props} data-cy="batch-select-item">
            {o.label}
          </ListItem>
        </>
      )}
      options={options}
      renderInput={(params) => {
        return (
          <TextField {...params} placeholder={t('batch_select_placeholder')} />
        );
      }}
      size="small"
      noOptionsText={t('batch_select_no_operation')}
      ListboxProps={{ style: { maxHeight: '80vh' } }}
    />
  );
};
