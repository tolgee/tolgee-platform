import { styled, IconButton, Tooltip, Checkbox, Box } from '@mui/material';
import { useTranslate } from '@tolgee/react';
import { useState } from 'react';
import { useGlobalLoading } from 'tg.component/GlobalLoading';
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
import { OperationAutoTranslate } from './OperationAutoTranslate';
import { BatchActions, BatchJobModel } from './types';

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
  gap: 10px;
  flex-wrap: wrap;
  align-items: start;
  box-sizing: border-box;
  position: relative;
  transition: background-color 300ms ease-in-out, visibility 0ms;
  padding: ${({ theme }) => theme.spacing(0.5, 2, 0.5, 2)};
  pointer-events: all;
  border-radius: 6px;
  background-color: ${({ theme }) => theme.palette.emphasis[200]};
  -webkit-box-shadow: 2px 2px 5px rgba(0, 0, 0, 0.25);
  box-shadow: 2px 2px 5px rgba(0, 0, 0, 0.25);
`;

const StyledBase = styled('div')`
  display: flex;
  flex-wrap: wrap;
  gap: 10px;
`;

const StyledToggleAllButton = styled(IconButton)`
  display: flex;
  flex-shrink: 1;
  width: 38px;
  height: 38px;
  margin-left: 3px;
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
      case 'auto_translate':
        return <OperationAutoTranslate {...sharedProps} />;
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

  useGlobalLoading(isLoading);

  return (
    <>
      {open && (
        <StyledContainer>
          <StyledContent>
            <StyledBase>
              <StyledItem>
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
            <Box>{pickOperation()}</Box>
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
