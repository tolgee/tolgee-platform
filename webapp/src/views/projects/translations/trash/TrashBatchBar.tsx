import React, { useState, useMemo } from 'react';
import {
  Autocomplete,
  Box,
  Checkbox,
  ListItem,
  Portal,
  TextField,
  Tooltip,
  styled,
  useTheme,
} from '@mui/material';
import { useTranslate } from '@tolgee/react';
import { getTextWidth } from 'tg.fixtures/getTextWidth';

import { useQueryClient } from 'react-query';
import { useProject } from 'tg.hooks/useProject';
import { invalidateUrlPrefix, useApiMutation } from 'tg.service/http/useQueryApi';
import { confirmation } from 'tg.hooks/confirmation';
import { BatchOperationDialog } from '../BatchOperations/OperationsSummary/BatchOperationDialog';
import { BatchOperationsSubmit } from '../BatchOperations/components/BatchOperationsSubmit';
import { BatchJobModel } from '../BatchOperations/types';

type TrashAction = 'restore' | 'hard_delete';

type TrashOperation = {
  id: TrashAction;
  label: string;
  enabled: boolean;
};

const StyledContainer = styled('div')`
  z-index: ${({ theme }) => theme.zIndex.drawer};
  position: fixed;
  display: flex;
  bottom: 0px;
  left: 0px;
  width: 100%;
  padding-left: 44px;
  pointer-events: none;
  transition: all 300ms ease-in-out;
  flex-shrink: 1;
  justify-content: flex-start;

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
  margin: ${({ theme }) => theme.spacing(2, 1, 2, 3)};
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

const StyledToggleAllButton = styled(Box)`
  width: 38px;
  height: 38px;
  margin: -1px -12px 0px -12.5px;
`;

type Props = {
  selectedKeys: number[];
  totalCount: number;
  allPageSelected: boolean;
  somePageSelected: boolean;
  onToggleSelectAll: () => void;
  onFinished: () => void;
  canRestore: boolean;
  canDelete: boolean;
};

export const TrashBatchBar = ({
  selectedKeys,
  totalCount,
  allPageSelected,
  somePageSelected,
  onToggleSelectAll,
  onFinished,
  canRestore,
  canDelete,
}: Props) => {
  const { t } = useTranslate();
  const theme = useTheme();
  const project = useProject();
  const queryClient = useQueryClient();
  const [operationId, setOperationId] = useState<TrashAction | undefined>();
  const [runningOperation, setRunningOperation] =
    useState<BatchJobModel | undefined>();

  const operations = useMemo<TrashOperation[]>(() => {
    const ops: TrashOperation[] = [];
    if (canRestore) {
      ops.push({
        id: 'restore',
        label: t('trash_batch_restore'),
        enabled: true,
      });
    }
    if (canDelete) {
      ops.push({
        id: 'hard_delete',
        label: t('trash_batch_hard_delete'),
        enabled: true,
      });
    }
    return ops;
  }, [canRestore, canDelete, t]);

  const restoreMutation = useApiMutation({
    url: '/v2/projects/{projectId}/start-batch-job/restore-keys',
    method: 'post',
  });

  const hardDeleteMutation = useApiMutation({
    url: '/v2/projects/{projectId}/start-batch-job/hard-delete-keys',
    method: 'post',
  });

  const mutateArgs = {
    path: { projectId: project.id },
    content: { 'application/json': { keyIds: selectedKeys } },
  };

  const handleSubmit = () => {
    if (!operationId) return;

    if (operationId === 'restore') {
      restoreMutation.mutate(mutateArgs, {
        onSuccess(data) {
          setRunningOperation(data);
        },
      });
    } else if (operationId === 'hard_delete') {
      confirmation({
        title: t('trash_batch_hard_delete_title'),
        message: t('trash_batch_hard_delete_confirmation', {
          count: String(selectedKeys.length),
        }),
        onConfirm() {
          hardDeleteMutation.mutate(mutateArgs, {
            onSuccess(data) {
              setRunningOperation(data);
            },
          });
        },
      });
    }
  };

  const handleFinished = () => {
    setRunningOperation(undefined);
    setOperationId(undefined);
    invalidateUrlPrefix(queryClient, '/v2/projects/{projectId}/keys/trash');
    onFinished();
  };

  const option = operations.find((o) => o.id === operationId);

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

  const isLoading = restoreMutation.isLoading || hardDeleteMutation.isLoading;

  if (selectedKeys.length === 0) {
    return null;
  }

  return (
    <>
      <Portal>
        <StyledContainer data-cy="trash-batch-bar">
          <StyledContent>
            <StyledBase>
              <StyledItem>
                <Tooltip
                  title={
                    allPageSelected
                      ? t('translations_clear_selection')
                      : t('translations_select_all')
                  }
                  disableInteractive
                >
                  <StyledToggleAllButton>
                    <Checkbox
                      data-cy="trash-select-all"
                      onClick={onToggleSelectAll}
                      size="small"
                      checked={selectedKeys.length > 0}
                      indeterminate={somePageSelected}
                    />
                  </StyledToggleAllButton>
                </Tooltip>
              </StyledItem>
              <StyledItem>{`${selectedKeys.length} / ${totalCount}`}</StyledItem>
              <StyledItem data-cy="trash-batch-select">
                <Autocomplete
                  sx={{ width: normalizedWidth }}
                  value={operations.find((o) => o.id === operationId) || null}
                  onChange={(_, value) => {
                    setOperationId(value?.id);
                  }}
                  renderOption={(props, o) => (
                    <React.Fragment key={o.id}>
                      <ListItem {...props} data-cy="trash-batch-select-item">
                        {o.label}
                      </ListItem>
                    </React.Fragment>
                  )}
                  options={operations}
                  getOptionLabel={(o) => o.label}
                  renderInput={(params) => (
                    <TextField
                      {...params}
                      placeholder={t('batch_select_placeholder')}
                    />
                  )}
                  size="small"
                />
              </StyledItem>
            </StyledBase>
            {operationId && (
              <BatchOperationsSubmit
                onClick={handleSubmit}
                loading={isLoading}
                disabled={!selectedKeys.length}
              />
            )}
          </StyledContent>
        </StyledContainer>
      </Portal>
      {runningOperation && (
        <BatchOperationDialog
          operation={runningOperation}
          onClose={() => setRunningOperation(undefined)}
          onFinished={handleFinished}
        />
      )}
    </>
  );
};
