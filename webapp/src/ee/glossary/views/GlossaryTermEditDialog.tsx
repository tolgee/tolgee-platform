import { Dialog, DialogTitle } from '@mui/material';
import { T } from '@tolgee/react';

import { useApiMutation, useApiQuery } from 'tg.service/http/useQueryApi';
import { messageService } from 'tg.service/MessageService';
import { components } from 'tg.service/apiSchema.generated';
import React, { useState } from 'react';
import { confirmation } from 'tg.hooks/confirmation';
import { usePreferredOrganization } from 'tg.globalContext/helpers';
import { useGlossary } from 'tg.ee.module/glossary/hooks/useGlossary';
import {
  GlossaryTermCreateEditForm,
  CreateOrUpdateGlossaryTermRequest,
} from 'tg.ee.module/glossary/components/GlossaryTermCreateEditForm';

type SimpleGlossaryTermModel = Omit<
  components['schemas']['SimpleGlossaryTermModel'],
  'id'
>;
type GlossaryTermTranslationModel = Omit<
  components['schemas']['GlossaryTermTranslationModel'],
  'languageTag'
>;

type Props = {
  open: boolean;
  onClose: () => void;
  onFinished: () => void;
  termId: number;
};

export const GlossaryTermEditDialog = ({
  open,
  onClose,
  onFinished,
  termId,
}: Props) => {
  const { preferredOrganization } = usePreferredOrganization();
  const glossary = useGlossary();

  const [initialTranslationValues, setInitialTranslationValues] = useState(
    undefined as GlossaryTermTranslationModel | undefined
  );

  const [initialTermValues, setInitialTermValues] = useState(
    undefined as SimpleGlossaryTermModel | undefined
  );

  const initialValues: CreateOrUpdateGlossaryTermRequest | undefined =
    initialTranslationValues &&
      initialTermValues && {
        text: initialTranslationValues.text ?? '',
        description: initialTermValues.description,
        flagNonTranslatable: initialTermValues.flagNonTranslatable,
        flagCaseSensitive: initialTermValues.flagCaseSensitive,
        flagAbbreviation: initialTermValues.flagAbbreviation,
        flagForbiddenTerm: initialTermValues.flagForbiddenTerm,
      };

  const mutation = useApiMutation({
    url: '/v2/organizations/{organizationId}/glossaries/{glossaryId}/terms/{termId}',
    method: 'put',
    invalidatePrefix:
      '/v2/organizations/{organizationId}/glossaries/{glossaryId}',
  });
  const save = (values: CreateOrUpdateGlossaryTermRequest) => {
    mutation.mutate(
      {
        path: {
          organizationId: preferredOrganization!.id,
          glossaryId: glossary.id,
          termId,
        },
        content: {
          'application/json': values,
        },
      },
      {
        onSuccess() {
          messageService.success(
            <T keyName="glossary_term_edit_success_message" />
          );
          onFinished();
        },
      }
    );
  };

  const onSave = async (values: CreateOrUpdateGlossaryTermRequest) => {
    if (
      initialTermValues?.flagNonTranslatable === false &&
      values.flagNonTranslatable
    ) {
      // User is changing term to non-translatable - we will delete all translations of this term
      await new Promise((resolve, reject) => {
        confirmation({
          title: (
            <T keyName="glossary_enable_non_translatable_flag_delete_term_translations_confirmation_title" />
          ),
          message: (
            <T keyName="glossary_enable_non_translatable_flag_delete_term_translations_confirmation_message" />
          ),
          onConfirm() {
            save(values);
            resolve(undefined);
          },
          onCancel() {
            reject(undefined);
          },
        });
      });
      return;
    }

    save(values);
  };

  const glossaryQuery = useApiQuery({
    url: '/v2/organizations/{organizationId}/glossaries/{glossaryId}',
    method: 'get',
    path: {
      organizationId: preferredOrganization!.id,
      glossaryId: glossary.id,
    },
    options: {
      onError(e) {
        onClose();
      },
    },
  });

  useApiQuery({
    url: '/v2/organizations/{organizationId}/glossaries/{glossaryId}/terms/{termId}',
    method: 'get',
    path: {
      organizationId: preferredOrganization!.id,
      glossaryId: glossary.id,
      termId,
    },
    options: {
      onSuccess(data) {
        setInitialTermValues?.(data);
      },
      onError(e) {
        onClose();
      },
    },
  });

  useApiQuery({
    url: '/v2/organizations/{organizationId}/glossaries/{glossaryId}/terms/{termId}/translations/{languageTag}',
    method: 'get',
    path: {
      organizationId: preferredOrganization!.id,
      glossaryId: glossary.id,
      termId,
      languageTag: glossaryQuery.data?.baseLanguageTag ?? '',
    },
    options: {
      enabled: glossaryQuery.data?.baseLanguageTag !== undefined,
      onSuccess(data) {
        setInitialTranslationValues?.(data);
      },
      onError(e) {
        onClose();
      },
    },
  });

  const deleteMutation = useApiMutation({
    url: '/v2/organizations/{organizationId}/glossaries/{glossaryId}/terms/{termId}',
    method: 'delete',
    invalidatePrefix:
      '/v2/organizations/{organizationId}/glossaries/{glossaryId}',
  });
  const onDelete = () => {
    confirmation({
      title: <T keyName="glossary_term_delete_confirmation_title" />,
      message: <T keyName="glossary_term_delete_confirmation_message" />,
      onConfirm() {
        deleteMutation.mutate(
          {
            path: {
              organizationId: preferredOrganization!.id,
              glossaryId: glossary.id,
              termId,
            },
          },
          {
            onSuccess() {
              messageService.success(
                <T keyName="glossary_term_delete_success_message" />
              );
              onClose();
            },
          }
        );
      },
    });
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
        <T keyName="glossary_term_edit_title" />
      </DialogTitle>

      <GlossaryTermCreateEditForm
        initialValues={initialValues}
        onClose={onClose}
        onSave={onSave}
        onDelete={onDelete}
        isSaving={mutation.isLoading}
      />
    </Dialog>
  );
};
