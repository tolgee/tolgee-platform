import React from 'react';
import { useApiMutation } from 'tg.service/http/useQueryApi';
import { confirmation } from 'tg.hooks/confirmation';
import { T, useTranslate } from '@tolgee/react';
import { SelectionService } from 'tg.service/useSelectionService';
import { messageService } from 'tg.service/MessageService';
import { TranslatedError } from 'tg.translationTools/TranslatedError';
import {
  useIsOrganizationOwnerOrMaintainer,
  usePreferredOrganization,
} from 'tg.globalContext/helpers';
import { useGlossary } from 'tg.ee.module/glossary/hooks/useGlossary';
import { BatchToolbarShell } from 'tg.component/entriesList/BatchToolbarShell';

type Props = {
  selectionService: SelectionService<number>;
};

export const GlossaryBatchToolbar: React.VFC<Props> = ({
  selectionService,
}) => {
  const { t } = useTranslate();
  const { preferredOrganization } = usePreferredOrganization();
  const glossary = useGlossary();

  const deleteSelectedMutation = useApiMutation({
    url: '/v2/organizations/{organizationId}/glossaries/{glossaryId}/terms',
    method: 'delete',
    invalidatePrefix:
      '/v2/organizations/{organizationId}/glossaries/{glossaryId}/terms',
  });

  const onDeleteSelected = () => {
    confirmation({
      title: <T keyName="glossary_term_batch_delete_confirmation_title" />,
      message: (
        <T
          keyName="glossary_term_batch_delete_confirmation_message"
          params={{ count: selectionService.selected.length }}
        />
      ),
      onConfirm: () => {
        deleteSelectedMutation.mutate(
          {
            path: {
              organizationId: preferredOrganization!.id,
              glossaryId: glossary.id,
            },
            content: {
              'application/json': {
                termIds: selectionService.selected,
              },
            },
          },
          {
            onSuccess() {
              selectionService.unselectAll();
            },
            onError(e) {
              messageService.error(
                <TranslatedError code={e.code || 'unexpected_error_occurred'} />
              );
            },
          }
        );
      },
    });
  };

  const canDelete = useIsOrganizationOwnerOrMaintainer();

  return (
    <BatchToolbarShell
      selectionService={selectionService}
      dataCy="glossary-batch-toolbar"
      actionDataCy="glossary-batch-delete-button"
      actionLabel={
        <T
          keyName="glossary_term_batch_delete_action"
          defaultValue="Delete terms"
        />
      }
      actionAriaLabel={t('glossary_term_batch_delete_action', 'Delete terms')}
      actionDisabled={!canDelete}
      actionLoading={deleteSelectedMutation.isLoading}
      onAction={onDeleteSelected}
    />
  );
};
