import { styled, Tooltip, Checkbox, Box } from '@mui/material';
import { useTranslate } from '@tolgee/react';
import { useState } from 'react';
import { useProjectActions } from 'tg.hooks/ProjectContext';

import {
  useTranslationsActions,
  useTranslationsSelector,
} from '../context/TranslationsContext';
import { BatchSelect } from './BatchSelect';
import { OperationAddTags } from './OperationAddTags';
import { OperationChangeNamespace } from './OperationChangeNamespace';
import { OperationClearTranslations } from './OperationClearTranslations';
import { OperationCopyTranslations } from './OperationCopyTranslations';
import { OperationDelete } from './OperationDelete';
import { OperationMarkAsReviewed } from './OperationMarkAsReviewed';
import { OperationMarkAsTranslated } from './OperationMarkAsTranslated';
import { OperationRemoveTags } from './OperationRemoveTags';
import { BatchOperationDialog } from './OperationsSummary/BatchOperationDialog';
import { OperationMachineTranslate } from './OperationMachineTranslate';
import { BatchActions, BatchJobModel } from './types';
import { OperationPreTranslate } from './OperationPreTranslate';

const StyledContainer = styled('div')`
  position: absolute;
  display: flex;
  bottom: 0px;
  right: 0px;
  width: 100%;
  transition: all 300ms ease-in-out;
  flex-shrink: 1;

  & .MuiInputBase-colorPrimary {
    background: ${({ theme }) => theme.palette.background.default};
  }
`;

const StyledContent = styled('div')`
  display: flex;
  gap: 12px;
  flex-wrap: wrap;
  align-items: start;
  box-sizing: border-box;
  position: relative;
  transition: background-color 300ms ease-in-out, visibility 0ms;
  padding: 6px 12px 6px 20px;
  margin-left: 10px;
  pointer-events: all;
  border-radius: 6px;
  background-color: ${({ theme }) =>
    theme.palette.mode === 'dark'
      ? theme.palette.emphasis[200]
      : theme.palette.emphasis[100]};
  -webkit-box-shadow: 2px 2px 5px rgba(0, 0, 0, 0.25);
  box-shadow: 2px 2px 5px rgba(0, 0, 0, 0.25);
`;

const StyledBase = styled('div')`
  display: flex;
  flex-wrap: wrap;
  gap: 12px;
`;

const StyledToggleAllButton = styled(Box)`
  width: 38px;
  height: 38px;
  margin: 0px -12px;
`;

const StyledItem = styled(Box)`
  height: 40px;
  display: flex;
  align-items: center;
  white-space: nowrap;
`;

type Props = {
  open: boolean;
  onClose: () => void;
};

export const BatchOperations = ({ open, onClose }: Props) => {
  const { t } = useTranslate();
  const selection = useTranslationsSelector((c) => c.selection);
  const totalCount = useTranslationsSelector((c) => c.translationsTotal || 0);
  const isLoading = useTranslationsSelector((c) => c.isLoadingAllIds);
  const isDeleting = useTranslationsSelector((c) => c.isDeleting);
  const { selectAll, selectionClear, refetchTranslations } =
    useTranslationsActions();
  const { refetchBatchJobs } = useProjectActions();

  const allSelected = totalCount === selection.length;
  const somethingSelected = !allSelected && Boolean(selection.length);
  const [operation, setOperation] = useState<BatchActions>();
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
    onStart: (operation: BatchJobModel) => {
      setRunningOperation(operation);
      refetchBatchJobs();
    },
  };

  function pickOperation() {
    switch (operation) {
      case 'delete':
        return <OperationDelete {...sharedProps} />;
      case 'machine_translate':
        return <OperationMachineTranslate {...sharedProps} />;
      case 'pre_translate':
        return <OperationPreTranslate {...sharedProps} />;
      case 'mark_as_translated':
        return <OperationMarkAsTranslated {...sharedProps} />;
      case 'mark_as_reviewed':
        return <OperationMarkAsReviewed {...sharedProps} />;
      case 'add_tags':
        return <OperationAddTags {...sharedProps} />;
      case 'remove_tags':
        return <OperationRemoveTags {...sharedProps} />;
      case 'change_namespace':
        return <OperationChangeNamespace {...sharedProps} />;
      case 'copy_translations':
        return <OperationCopyTranslations {...sharedProps} />;
      case 'clear_translations':
        return <OperationClearTranslations {...sharedProps} />;
    }
  }

  function onFinished() {
    refetchTranslations();
    selectionClear();
    setOperation(undefined);
    onClose();
  }

  return (
    <>
      {open && (
        <StyledContainer data-cy="batch-operations-section">
          <StyledContent>
            <StyledBase>
              <StyledItem>
                <Tooltip
                  title={
                    runningOperation
                      ? ''
                      : allSelected
                      ? t('translations_clear_selection')
                      : t('translations_select_all')
                  }
                  disableInteractive
                >
                  <StyledToggleAllButton>
                    <Checkbox
                      data-cy="translations-select-all-button"
                      disabled={isLoading || isDeleting}
                      onClick={handleToggleSelectAll}
                      size="small"
                      checked={Boolean(selection.length)}
                      indeterminate={somethingSelected}
                    />
                  </StyledToggleAllButton>
                </Tooltip>
              </StyledItem>
              <StyledItem>{`${selection.length} / ${totalCount}`}</StyledItem>
              <StyledItem data-cy="batch-operations-select">
                <BatchSelect
                  value={operation}
                  onChange={setOperation}
                  {...sharedProps}
                />
              </StyledItem>
            </StyledBase>
            {pickOperation()}
          </StyledContent>
        </StyledContainer>
      )}
      {runningOperation && (
        <BatchOperationDialog
          operation={runningOperation}
          onClose={() => setRunningOperation(undefined)}
          onFinished={onFinished}
        />
      )}
    </>
  );
};
