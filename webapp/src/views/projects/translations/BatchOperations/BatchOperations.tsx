import { Box, styled } from '@mui/material';
import { useState } from 'react';
import { useProjectActions } from 'tg.hooks/ProjectContext';

import {
  useTranslationsActions,
  useTranslationsSelector,
} from '../context/TranslationsContext';
import { BatchSelect } from './BatchSelect';
import { BatchOperationDialog } from './OperationsSummary/BatchOperationDialog';
import { BatchActions, BatchJobModel } from './types';
import { SelectAllCheckbox } from './SelectAllCheckbox';
import { useBatchOperations } from './operations';

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
      : theme.palette.emphasis[50]};
  -webkit-box-shadow: 2px 2px 5px rgba(0, 0, 0, 0.25);
  box-shadow: 2px 2px 5px rgba(0, 0, 0, 0.25);
`;

const StyledBase = styled('div')`
  display: flex;
  flex-wrap: wrap;
  gap: 12px;
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
  const selection = useTranslationsSelector((c) => c.selection);
  const totalCount = useTranslationsSelector((c) => c.translationsTotal || 0);
  const { selectionClear, refetchTranslations } = useTranslationsActions();
  const { refetchBatchJobs } = useProjectActions();

  const [operationId, setOperationId] = useState<BatchActions>();
  const [runningOperation, setRunningOperation] = useState<BatchJobModel>();

  const { findOperation } = useBatchOperations();

  function onCloseOnly() {
    selectionClear();
    setOperationId(undefined);
    onClose();
  }

  function onFinished() {
    refetchTranslations();
    selectionClear();
    setOperationId(undefined);
    onClose();
  }

  const sharedProps = {
    disabled: !selection.length,
    onStart: (operation: BatchJobModel) => {
      setRunningOperation(operation);
      refetchBatchJobs();
    },
    onClose: onCloseOnly,
    onFinished,
  };

  const operation = findOperation(operationId);
  const OperationComponent = operation?.component;

  return (
    <>
      {open && (
        <StyledContainer data-cy="batch-operations-section">
          <StyledContent>
            <StyledBase>
              <StyledItem>
                <SelectAllCheckbox />
              </StyledItem>
              <StyledItem>{`${selection.length} / ${totalCount}`}</StyledItem>
              <StyledItem data-cy="batch-operations-select">
                <BatchSelect
                  value={operationId}
                  onChange={setOperationId}
                  {...sharedProps}
                />
              </StyledItem>
            </StyledBase>
            {OperationComponent && <OperationComponent {...sharedProps} />}
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
