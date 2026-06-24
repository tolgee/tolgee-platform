import React from 'react';
import { T, useTranslate } from '@tolgee/react';
import { confirmation } from 'tg.hooks/confirmation';
import { useApiMutation } from 'tg.service/http/useQueryApi';
import { messageService } from 'tg.service/MessageService';
import { SelectionService } from 'tg.service/useSelectionService';
import { TranslatedError } from 'tg.translationTools/TranslatedError';
import { useIsOrganizationOwnerOrMaintainer } from 'tg.globalContext/helpers';
import { BatchToolbarShell } from 'tg.component/entriesList/BatchToolbarShell';

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
  const { t } = useTranslate();
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
    <BatchToolbarShell
      selectionService={selectionService}
      dataCy="tm-batch-toolbar"
      actionDataCy="tm-batch-delete-button"
      actionLabel={
        <T
          keyName="translation_memory_entries_batch_delete_action"
          defaultValue="Delete entries"
        />
      }
      actionAriaLabel={t(
        'translation_memory_entries_batch_delete_action',
        'Delete entries'
      )}
      actionDisabled={!canDelete}
      actionLoading={deleteSelectedMutation.isLoading}
      onAction={onDeleteSelected}
    />
  );
};
