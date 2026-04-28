import React from 'react';
import {
  Card,
  Checkbox,
  MenuItem,
  Select,
  styled,
  Typography,
} from '@mui/material';
import { ChevronRight } from '@untitled-ui/icons-react';
import { T } from '@tolgee/react';
import LoadingButton from 'tg.component/common/form/LoadingButton';
import { confirmation } from 'tg.hooks/confirmation';
import { useApiMutation } from 'tg.service/http/useQueryApi';
import { messageService } from 'tg.service/MessageService';
import { SelectionService } from 'tg.service/useSelectionService';
import { TranslatedError } from 'tg.translationTools/TranslatedError';
import { useIsOrganizationOwnerOrMaintainer } from 'tg.globalContext/helpers';

const StyledCard = styled(Card)`
  display: flex;
  align-items: center;
  flex-wrap: wrap;
  flex-direction: row;
  gap: ${({ theme }) => theme.spacing(1.5)};
  padding: ${({ theme }) => theme.spacing(1, 1.5)};
  margin: ${({ theme }) => theme.spacing(2, 1)};
  margin-left: ${({ theme }) => theme.spacing(5)};
  border-radius: ${({ theme }) => theme.spacing(1)};
  background-color: ${({ theme }) =>
    theme.palette.mode === 'dark'
      ? theme.palette.emphasis[200]
      : theme.palette.emphasis[50]};
  transition: background-color 300ms ease-in-out, visibility 0ms;
  -webkit-box-shadow: 2px 2px 5px rgba(0, 0, 0, 0.25);
  box-shadow: 2px 2px 5px rgba(0, 0, 0, 0.25);
`;

const StyledCheckbox = styled(Checkbox)`
  margin: ${({ theme }) => theme.spacing(0, -1.5, 0, -1)};
`;

type Props = {
  organizationId: number;
  translationMemoryId: number;
  selectionService: SelectionService<number>;
};

export const TmBatchToolbar: React.VFC<Props> = ({
  organizationId,
  translationMemoryId,
  selectionService,
}) => {
  const canDelete = useIsOrganizationOwnerOrMaintainer();

  const deleteSelectedMutation = useApiMutation({
    url: '/v2/organizations/{organizationId}/translation-memories/{translationMemoryId}/entries',
    method: 'delete',
    invalidatePrefix:
      '/v2/organizations/{organizationId}/translation-memories/{translationMemoryId}/entries',
  });

  const onDeleteSelected = () => {
    confirmation({
      title: (
        <T
          keyName="translation_memory_entries_batch_delete_title"
          defaultValue="Delete entries"
        />
      ),
      message: (
        <T
          keyName="translation_memory_entries_batch_delete_message"
          defaultValue="Delete {count, plural, one {# selected entry} other {# selected entries}}? This will remove all languages for each selected row."
          params={{ count: selectionService.selected.length }}
        />
      ),
      onConfirm: () => {
        deleteSelectedMutation.mutate(
          {
            path: { organizationId, translationMemoryId },
            content: {
              'application/json': {
                entryIds: selectionService.selected,
              },
            },
          },
          {
            onSuccess: () => {
              selectionService.unselectAll();
            },
            onError: (e) => {
              messageService.error(
                <TranslatedError code={e.code || 'unexpected_error_occurred'} />
              );
            },
          }
        );
      },
    });
  };

  return (
    <StyledCard
      data-cy="tm-batch-toolbar"
      sx={{
        visibility: selectionService.selected.length > 0 ? 'visible' : 'hidden',
      }}
    >
      <StyledCheckbox
        size="small"
        checked={selectionService.isAllSelected}
        onChange={selectionService.toggleSelectAll}
        indeterminate={selectionService.isSomeSelected}
        disabled={selectionService.isLoading}
      />
      <Typography>{`${selectionService.selected.length} / ${selectionService.total}`}</Typography>
      <Select variant="outlined" size="small" value={0} sx={{ minWidth: 250 }}>
        <MenuItem value={0}>
          <T
            keyName="translation_memory_entries_batch_delete_action"
            defaultValue="Delete entries"
          />
        </MenuItem>
      </Select>
      <LoadingButton
        disableElevation
        disabled={!canDelete}
        variant="contained"
        color="primary"
        sx={{ minWidth: 0, minHeight: 0, width: 40, height: 40, padding: 0 }}
        loading={deleteSelectedMutation.isLoading || selectionService.isLoading}
        onClick={onDeleteSelected}
        data-cy="tm-batch-delete-button"
      >
        <ChevronRight width={20} height={20} />
      </LoadingButton>
    </StyledCard>
  );
};
