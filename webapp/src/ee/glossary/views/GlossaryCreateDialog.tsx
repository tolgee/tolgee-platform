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
import { useHistory } from 'react-router-dom';
import { LINKS, PARAMS } from 'tg.constants/links';
import {
  GlossaryCreateEditForm,
  CreateEditGlossaryFormValues,
} from 'tg.ee.module/glossary/components/GlossaryCreateEditForm';

type CreateGlossaryRequest = components['schemas']['CreateGlossaryRequest'];

const glossaryInitialValues: CreateEditGlossaryFormValues = {
  name: '',
  baseLanguage: undefined,
  assignedProjects: [],
};

type Props = {
  open: boolean;
  onClose: () => void;
  onFinished: () => void;
};

export const GlossaryCreateDialog = ({ open, onClose, onFinished }: Props) => {
  const { t } = useTranslate();
  const history = useHistory();

  const { preferredOrganization } = usePreferredOrganization();

  const { isEnabled } = useEnabledFeatures();
  const glossaryFeatureEnabled = isEnabled('GLOSSARY');

  const mutation = useApiMutation({
    url: '/v2/organizations/{organizationId}/glossaries',
    method: 'post',
    invalidatePrefix: '/v2/organizations/{organizationId}/glossaries',
  });
  const save = async (values: CreateEditGlossaryFormValues) => {
    const data: CreateGlossaryRequest = {
      name: values.name,
      baseLanguageTag: values.baseLanguage!.tag,
      assignedProjectIds: values.assignedProjects?.map(({ id }) => id),
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
        onSuccess({ id }) {
          messageService.success(
            <T keyName="glossary_create_success_message" />
          );
          history.push(
            LINKS.ORGANIZATION_GLOSSARY.build({
              [PARAMS.GLOSSARY_ID]: id,
              [PARAMS.ORGANIZATION_SLUG]: preferredOrganization!.slug,
            })
          );
          onFinished();
        },
      }
    );
  };

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
        <T keyName="glossary_create_title" />
      </DialogTitle>

      <GlossaryCreateEditForm
        initialValues={glossaryInitialValues}
        onClose={onClose}
        onSave={save}
        isSaving={mutation.isLoading}
      />
    </Dialog>
  );
};
