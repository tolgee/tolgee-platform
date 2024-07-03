import React, { useMemo } from 'react';
import {
  Autocomplete,
  ListItem,
  TextField,
  styled,
  useTheme,
} from '@mui/material';
import { useTranslate } from '@tolgee/react';
import { getTextWidth } from 'tg.fixtures/getTextWidth';
import { useProjectPermissions } from 'tg.hooks/useProjectPermissions';

import { BatchActions } from './types';
import { useTranslationsSelector } from '../context/TranslationsContext';
import { useEnabledFeatures } from 'tg.globalContext/helpers';

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
  const { satisfiesPermission } = useProjectPermissions();
  const prefilteredTask = useTranslationsSelector(
    (c) => c.prefilter?.task !== undefined
  );
  const canEditKey = satisfiesPermission('keys.edit');
  const canDeleteKey = satisfiesPermission('keys.delete');
  const canMachineTranslate = satisfiesPermission('translations.batch-machine');
  const canPretranslate = satisfiesPermission('translations.batch-by-tm');
  const canChangeState = satisfiesPermission('translations.state-edit');
  const canViewTranslations = satisfiesPermission('translations.view');
  const canEditTranslations = satisfiesPermission('translations.edit');
  const canEditTasks = satisfiesPermission('tasks.edit');

  const { features } = useEnabledFeatures();
  const taskFeature = features.includes('TASKS');

  const options: {
    id: BatchActions;
    label: string;
    divider?: boolean;
    enabled?: boolean;
    hidden?: boolean;
  }[] = [
    {
      id: 'machine_translate',
      label: t('batch_operations_machine_translate'),
      enabled: canMachineTranslate,
    },
    {
      id: 'pre_translate',
      label: t('batch_operations_pre_translate'),
      enabled: canPretranslate,
    },
    {
      id: 'mark_as_translated',
      label: t('batch_operations_mark_as_translated'),
      enabled: canChangeState,
    },
    {
      id: 'mark_as_reviewed',
      label: t('batch_operations_mark_as_reviewed'),
      enabled: canChangeState,
    },
    {
      id: 'copy_translations',
      label: t('batch_operations_copy_translations'),
      enabled: canEditTranslations,
    },
    {
      id: 'clear_translations',
      label: t('batch_operation_clear_translations'),
      enabled: canEditTranslations,
    },
    {
      id: 'export_translations',
      label: t('batch_operations_export_translations'),
      enabled: canViewTranslations,
    },
    {
      id: 'task_create',
      label: t('batch_operations_create_task'),
      divider: true,
      enabled: canEditTasks,
      hidden: !taskFeature,
    },
    {
      id: 'task_add_keys',
      label: t('batch_operations_task_add_keys'),
      enabled: canEditTasks,
      hidden: prefilteredTask || !taskFeature,
    },
    {
      id: 'task_remove_keys',
      label: t('batch_operations_task_remove_keys'),
      enabled: canEditTasks,
      hidden: !prefilteredTask || !taskFeature,
    },
    {
      id: 'add_tags',
      label: t('batch_operations_add_tags'),
      divider: true,
      enabled: canEditKey,
    },
    {
      id: 'remove_tags',
      label: t('batch_operations_remove_tags'),
      enabled: canEditKey,
    },
    {
      id: 'change_namespace',
      label: t('batch_operations_change_namespace'),
      enabled: canEditKey,
    },
    {
      id: 'delete',
      label: t('batch_operations_delete'),
      enabled: canDeleteKey,
    },
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
        <React.Fragment key={o.id}>
          {o.divider && <StyledSeparator />}
          {o.enabled === false ? (
            <ListItem data-cy="batch-select-item" disabled={true}>
              {o.label}
            </ListItem>
          ) : (
            <ListItem {...props} data-cy="batch-select-item">
              {o.label}
            </ListItem>
          )}
        </React.Fragment>
      )}
      options={options.filter((o) => !o.hidden)}
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
