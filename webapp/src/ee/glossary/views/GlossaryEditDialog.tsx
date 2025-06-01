import { Dialog, DialogTitle } from '@mui/material';
import { T, useTranslate } from '@tolgee/react';

import { components } from 'tg.service/apiSchema.generated';
import { useApiMutation, useApiQuery } from 'tg.service/http/useQueryApi';
import {
  useEnabledFeatures,
  usePreferredOrganization,
} from 'tg.globalContext/helpers';
import { DisabledFeatureBanner } from 'tg.component/common/DisabledFeatureBanner';
import { messageService } from 'tg.service/MessageService';
import { useState } from 'react';
import { languageInfo } from '@tginternal/language-util/lib/generated/languageInfo';
import {
  GlossaryCreateEditForm,
  CreateEditGlossaryFormValues,
} from 'tg.ee.module/glossary/components/GlossaryCreateEditForm';

type UpdateGlossaryRequest = components['schemas']['UpdateGlossaryRequest'];

type Props = {
  open: boolean;
  onClose: () => void;
  onFinished: () => void;
  glossaryId: number;
};

export const GlossaryEditDialog = ({
  open,
  onClose,
  onFinished,
  glossaryId,
}: Props) => {
  const { t } = useTranslate();

  const { preferredOrganization } = usePreferredOrganization();

  const { isEnabled } = useEnabledFeatures();
  const glossaryFeature = isEnabled('GLOSSARY');

  const mutation = useApiMutation({
    url: '/v2/organizations/{organizationId}/glossaries/{glossaryId}',
    method: 'put',
    invalidatePrefix: '/v2/organizations/{organizationId}/glossaries',
  });
  const save = async (values: CreateEditGlossaryFormValues) => {
    const data: UpdateGlossaryRequest = {
      name: values.name,
      baseLanguageTag: values.baseLanguage!.tag,
      assignedProjectIds: values.assignedProjects?.map(({ id }) => id) || [],
    };

    mutation.mutate(
      {
        path: {
          organizationId: preferredOrganization!.id,
          glossaryId,
        },
        content: {
          'application/json': data,
        },
      },
      {
        onSuccess() {
          messageService.success(<T keyName="glossary_edit_success_message" />);
          onFinished();
        },
      }
    );
  };

  const [initialValues, setInitialValues] = useState(
    undefined as CreateEditGlossaryFormValues | undefined
  );

  useApiQuery({
    url: '/v2/organizations/{organizationId}/glossaries/{glossaryId}',
    method: 'get',
    path: {
      organizationId: preferredOrganization!.id,
      glossaryId,
    },
    options: {
      onSuccess(data) {
        const language = data.baseLanguageTag
          ? {
              tag: data.baseLanguageTag,
              flagEmoji: languageInfo[data.baseLanguageTag]?.flags?.[0] || '',
              name:
                languageInfo[data.baseLanguageTag]?.englishName ||
                data.baseLanguageTag,
            }
          : undefined;
        setInitialValues?.({
          name: data.name,
          baseLanguage: language,
          assignedProjects:
            data.assignedProjects._embedded?.projects?.map((p) => ({
              id: p.id,
              name: p.name,
            })) || [],
        });
      },
      onError(e) {
        onClose();
      },
    },
  });

  return (
    <Dialog
      data-cy="create-edit-glossary-dialog"
      open={open}
      onClose={onClose}
      maxWidth="sm"
      onClick={(e) => e.stopPropagation()}
    >
      {!glossaryFeature && (
        <DisabledFeatureBanner
          customMessage={t('glossaries_feature_description')}
        />
      )}
      <DialogTitle>
        <T keyName="glossary_edit_title" />
      </DialogTitle>

      <GlossaryCreateEditForm
        initialValues={initialValues}
        onClose={onClose}
        onSave={save}
        isSaving={mutation.isLoading}
        isEditing
      />
    </Dialog>
  );
};
