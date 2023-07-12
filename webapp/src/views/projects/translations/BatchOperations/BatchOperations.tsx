import { styled, IconButton, Tooltip, Checkbox, Box } from '@mui/material';
import { T, useTranslate } from '@tolgee/react';
import { useState } from 'react';
import { useGlobalLoading } from 'tg.component/GlobalLoading';

import {
  useTranslationsActions,
  useTranslationsSelector,
} from '../context/TranslationsContext';
import { BatchSelect } from './BatchSelect';
import { OperationDelete } from './OperationDelete';
import { BatchOperationDialog } from './OperationsSummary/BatchOperationDialog';
import { OperationTranslate } from './OperationTranslate';
import { BatchActions, BatchJobModel } from './types';

const StyledContainer = styled('div')`
  position: absolute;
  display: flex;
  bottom: 0px;
  right: 0px;
  width: 100%;
  transition: all 300ms ease-in-out;
  flex-shrink: 1;
`;

const StyledContent = styled('div')`
  display: grid;
  gap: 10px;
  align-items: center;
  grid-auto-flow: column;
  box-sizing: border-box;
  position: relative;
  transition: background-color 300ms ease-in-out, visibility 0ms;
  padding: ${({ theme }) => theme.spacing(0.5, 2, 0.5, 2)};
  pointer-events: all;
  border-radius: 6px;
  max-width: 100%;
  background-color: ${({ theme }) => theme.palette.emphasis[200]};
  -webkit-box-shadow: 2px 2px 5px rgba(0, 0, 0, 0.25);
  box-shadow: 2px 2px 5px rgba(0, 0, 0, 0.25);
`;

const StyledToggleAllButton = styled(IconButton)`
  display: flex;
  flex-shrink: 1;
  width: 38px;
  height: 38px;
  margin-left: 3px;
`;

export const BatchOperations = () => {
  const { t } = useTranslate();
  const selection = useTranslationsSelector((c) => c.selection);
  const totalCount = useTranslationsSelector((c) => c.translationsTotal || 0);
  const isLoading = useTranslationsSelector((c) => c.isLoadingAllIds);
  const isDeleting = useTranslationsSelector((c) => c.isDeleting);
  const { selectAll, selectionClear } = useTranslationsActions();

  const allSelected = totalCount === selection.length;
  const somethingSelected = !allSelected && Boolean(selection.length);
  const [operation, setOperation] = useState<BatchActions>('delete');
  const [runningOperation, setRunningOperation] = useState<BatchJobModel>();

  const handleToggleSelectAll = () => {
    if (!allSelected) {
      selectAll();
    } else {
      selectionClear();
    }
  };

  const sharedProps = {
    disabled: !selection.length,
    onStart: (operation: BatchJobModel) => setRunningOperation(operation),
  };

  function pickOperation() {
    switch (operation) {
      case 'delete':
        return <OperationDelete {...sharedProps} />;
      case 'translate':
        return <OperationTranslate {...sharedProps} />;
    }
  }

  useGlobalLoading(isLoading);

  return (
    <StyledContainer>
      <StyledContent>
        <Tooltip
          title={
            allSelected
              ? t('translations_clear_selection')
              : t('translations_select_all')
          }
        >
          <StyledToggleAllButton
            onClick={handleToggleSelectAll}
            data-cy="translations-select-all-button"
            size="small"
          >
            <Checkbox
              disabled={isLoading || isDeleting}
              size="small"
              checked={Boolean(selection.length)}
              indeterminate={somethingSelected}
            />
          </StyledToggleAllButton>
        </Tooltip>
        <T
          keyName="translations_selected_count"
          params={{ count: selection.length, total: totalCount }}
        />
        <Box py={0.5}>
          <BatchSelect
            value={operation}
            onChange={setOperation}
            {...sharedProps}
          />
        </Box>
        <Box>{pickOperation()}</Box>
      </StyledContent>
      {runningOperation && (
        <BatchOperationDialog operation={runningOperation} />
      )}
    </StyledContainer>
  );
};
