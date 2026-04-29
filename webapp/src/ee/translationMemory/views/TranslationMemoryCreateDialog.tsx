import { Dialog, DialogTitle } from '@mui/material';
import { T, useTranslate } from '@tolgee/react';

import { components } from 'tg.service/apiSchema.generated';
import { useApiMutation } from 'tg.service/http/useQueryApi';
import {
  useEnabledFeatures,
  usePreferredOrganization,
} from 'tg.globalContext/helpers';
import { DisabledFeatureBanner } from 'tg.component/common/DisabledFeatureBanner';
import { messageService } from 'tg.service/MessageService';
import {
  TranslationMemoryCreateEditForm,
  CreateEditTranslationMemoryFormValues,
} from 'tg.ee.module/translationMemory/components/TranslationMemoryCreateEditForm';

type CreateRequest =
  components['schemas']['CreateSharedTranslationMemoryRequest'];

const initialValues: CreateEditTranslationMemoryFormValues = {
  name: '',
  baseLanguage: undefined,
  defaultPenalty: 0,
  writeOnlyReviewed: false,
  assignedProjects: [],
};

type Props = {
  open: boolean;
  onClose: () => void;
  onFinished: () => void;
};

export const TranslationMemoryCreateDialog = ({
  open,
  onClose,
  onFinished,
}: Props) => {
  const { t } = useTranslate();

  const { preferredOrganization } = usePreferredOrganization();

  const { isEnabled } = useEnabledFeatures();
  const featureEnabled = isEnabled('TRANSLATION_MEMORY');

  const mutation = useApiMutation({
    url: '/v2/organizations/{organizationId}/translation-memories',
    method: 'post',
    invalidatePrefix: '/v2/organizations/{organizationId}/translation-memories',
  });

  const save = async (values: CreateEditTranslationMemoryFormValues) => {
    const data: CreateRequest = {
      name: values.name,
      sourceLanguageTag: values.baseLanguage!.tag,
      defaultPenalty: values.defaultPenalty,
      writeOnlyReviewed: values.writeOnlyReviewed,
      assignedProjects: values.assignedProjects.map((a) => ({
        projectId: a.projectId,
        readAccess: a.readAccess,
        writeAccess: a.writeAccess,
        penalty: a.penalty ?? undefined,
      })),
    };

    mutation.mutate(
      {
        path: {
          organizationId: preferredOrganization!.id,
        },
        content: {
          'application/json': data,
        },
      },
      {
        onSuccess() {
          messageService.success(
            <T
              keyName="translation_memory_create_success_message"
              defaultValue="Translation memory created"
            />
          );
          onFinished();
        },
      }
    );
  };

  return (
    <Dialog
      data-cy="create-translation-memory-dialog"
      open={open}
      onClose={onClose}
      maxWidth="sm"
      onClick={(e) => e.stopPropagation()}
    >
      {!featureEnabled && (
        <DisabledFeatureBanner
          customMessage={t(
            'translation_memories_feature_description',
            'Translation memory management is available on the Business plan.'
          )}
        />
      )}
      <DialogTitle>
        <T
          keyName="translation_memory_create_title"
          defaultValue="New translation memory"
        />
      </DialogTitle>

      <TranslationMemoryCreateEditForm
        mode="create"
        initialValues={initialValues}
        onClose={onClose}
        onSave={save}
        isSaving={mutation.isLoading}
      />
    </Dialog>
  );
};
