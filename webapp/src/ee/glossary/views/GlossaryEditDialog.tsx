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
import { languageInfo } from '@tginternal/language-util/lib/generated/languageInfo';
import {
  GlossaryCreateEditForm,
  CreateEditGlossaryFormValues,
} from 'tg.ee.module/glossary/components/GlossaryCreateEditForm';
import { useGlossary } from 'tg.ee.module/glossary/hooks/useGlossary';
import { useMemo } from 'react';

type UpdateGlossaryRequest = components['schemas']['UpdateGlossaryRequest'];

type Props = {
  open: boolean;
  onClose: () => void;
  onFinished: () => void;
};

export const GlossaryEditDialog = ({ open, onClose, onFinished }: Props) => {
  const { t } = useTranslate();
  const glossary = useGlossary();

  const { preferredOrganization } = usePreferredOrganization();

  const { isEnabled } = useEnabledFeatures();
  const glossaryFeatureEnabled = isEnabled('GLOSSARY');

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
          glossaryId: glossary.id,
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

  const initialValues: CreateEditGlossaryFormValues = useMemo(() => {
    const language = glossary.baseLanguageTag
      ? {
          tag: glossary.baseLanguageTag,
          flagEmoji: languageInfo[glossary.baseLanguageTag]?.flags?.[0] || '',
          name:
            languageInfo[glossary.baseLanguageTag]?.englishName ||
            glossary.baseLanguageTag,
        }
      : undefined;
    return {
      name: glossary.name,
      baseLanguage: language,
      assignedProjects: glossary.assignedProjects.map((p) => ({
        id: p.id,
        name: p.name,
      })),
    };
  }, [glossary]);

  return (
    <Dialog
      data-cy="create-edit-glossary-dialog"
      open={open}
      onClose={onClose}
      maxWidth="sm"
      onClick={(e) => e.stopPropagation()}
    >
      {!glossaryFeatureEnabled && (
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
