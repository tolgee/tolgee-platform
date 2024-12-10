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
import { BatchOperation, useBatchOperations } from './operations';
import { BatchActions } from './types';
import { useProject } from 'tg.hooks/useProject';

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
  const project = useProject();
  const { operations } = useBatchOperations();

  hideChangeNamespaceOperationIfNamespacesAreDisabled(
    project.useNamespaces,
    operations
  );

  const option = operations.find((o) => o.id === value);

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
      value={operations.find((o) => o.id === value) || null}
      onChange={(_, value) => {
        onChange(value?.id);
      }}
      renderOption={(props, o) => (
        <React.Fragment key={o.id}>
          {o.divider && <StyledSeparator />}
          {!o.enabled ? (
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
      options={operations.filter((o) => !o.hidden)}
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

function hideChangeNamespaceOperationIfNamespacesAreDisabled(
  useNamespaces: boolean,
  operations: BatchOperation[]
) {
  const changeNamespaceOperation = operations.find(
    (operation) => operation.id === 'change_namespace'
  );
  if (changeNamespaceOperation != undefined) {
    changeNamespaceOperation.hidden = !useNamespaces;
  }
}
