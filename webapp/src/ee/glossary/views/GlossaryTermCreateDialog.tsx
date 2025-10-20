import { Dialog, DialogTitle } from '@mui/material';
import { T } from '@tolgee/react';

import { useApiMutation } from 'tg.service/http/useQueryApi';
import { messageService } from 'tg.service/MessageService';
import React from 'react';
import { useGlossary } from 'tg.ee.module/glossary/hooks/useGlossary';
import {
  GlossaryTermCreateEditForm,
  CreateOrUpdateGlossaryTermRequest,
} from 'tg.ee.module/glossary/components/GlossaryTermCreateEditForm';

type Props = {
  open: boolean;
  onClose: () => void;
  onFinished: () => void;
};

const termInitialValues: CreateOrUpdateGlossaryTermRequest = {
  text: '',
  description: '',
  flagNonTranslatable: false,
  flagCaseSensitive: false,
  flagAbbreviation: false,
  flagForbiddenTerm: false,
};

export const GlossaryTermCreateDialog = ({
  open,
  onClose,
  onFinished,
}: Props) => {
  const glossary = useGlossary();

  const mutation = useApiMutation({
    url: '/v2/organizations/{organizationId}/glossaries/{glossaryId}/terms',
    method: 'post',
    invalidatePrefix:
      '/v2/organizations/{organizationId}/glossaries/{glossaryId}',
  });
  const save = async (values: CreateOrUpdateGlossaryTermRequest) => {
    mutation.mutate(
      {
        path: {
          organizationId: glossary.organizationOwner.id,
          glossaryId: glossary.id,
        },
        content: {
          'application/json': values,
        },
      },
      {
        onSuccess() {
          messageService.success(
            <T keyName="glossary_term_create_success_message" />
          );
          onFinished();
        },
      }
    );
  };

  return (
    <Dialog
      open={open}
      onClose={onClose}
      maxWidth="sm"
      fullWidth
      onClick={(e) => e.stopPropagation()}
      data-cy="create-glossary-term-dialog"
    >
      <DialogTitle>
        <T keyName="glossary_term_create_title" />
      </DialogTitle>

      <GlossaryTermCreateEditForm
        initialValues={termInitialValues}
        onClose={onClose}
        onSave={save}
        isSaving={mutation.isLoading}
      />
    </Dialog>
  );
};
