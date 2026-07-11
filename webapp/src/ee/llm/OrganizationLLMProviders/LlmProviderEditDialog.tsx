import {
  Button,
  Dialog,
  DialogActions,
  DialogContent,
  styled,
} from '@mui/material';
import { Formik } from 'formik';
import { LlmProviderForm } from './LlmProviderEdit/LlmProviderForm';
import { T, useTranslate } from '@tolgee/react';
import { LoadingButton } from '@mui/lab';
import { useApiMutation } from 'tg.service/http/useQueryApi';
import { useOrganization } from '../../../views/organizations/useOrganization';
import { messageService } from 'tg.service/MessageService';
import {
  getInitialValues,
  getValidationSchema,
  LlmProviderModel,
  LlmProviderType,
} from './LlmProviderEdit/llmProvidersConfig';
import { useState } from 'react';
import { LlmDialogTitleWithLink } from './LlmDialogTitleWithLink';

const StyledDialogContent = styled(DialogContent)`
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 0px 16px;
  width: 85vw;
  max-width: 700px;
  @media (max-width: 600px) {
    grid-template-columns: 1fr;
  }
`;

type Props = {
  onClose: () => void;
  provider: LlmProviderModel;
};

export const LlmProviderEditDialog = ({ onClose, provider }: Props) => {
  const { t } = useTranslate();
  const organization = useOrganization();
  const [type, setType] = useState<LlmProviderType>(
    provider.type as Exclude<LlmProviderType, 'TOLGEE'>
  );

  const updateLoadable = useApiMutation({
    url: '/v2/organizations/{organizationId}/llm-providers/{providerId}',
    method: 'put',
    invalidatePrefix: '/v2/organizations/{organizationId}/llm-providers',
  });

  return (
    <Dialog open={true} onClose={onClose} maxWidth="xl">
      <Formik
        initialValues={getInitialValues(type, t, provider)}
        enableReinitialize
        isInitialValid={false}
        onSubmit={(values) => {
          updateLoadable.mutate(
            {
              content: { 'application/json': { ...values, type } },
              path: {
                providerId: provider.id,
                organizationId: organization!.id,
              },
            },
            {
              onSuccess() {
                messageService.success(
                  <T keyName="llm_provider_update_success_message" />
                );
                onClose();
              },
            }
          );
        }}
        validationSchema={getValidationSchema(type, t)}
      >
        {({ submitForm, dirty }) => {
          return (
            <>
              <LlmDialogTitleWithLink title={t('llm_provider_update_title')} />
              <StyledDialogContent>
                <LlmProviderForm type={type} onTypeChange={setType} />
              </StyledDialogContent>
              <DialogActions>
                <Button onClick={onClose}>{t('global_cancel_button')}</Button>
                <LoadingButton
                  onClick={submitForm}
                  loading={updateLoadable.isLoading}
                  disabled={!dirty && provider.type === type}
                  color="primary"
                  variant="contained"
                  data-cy="llm-provider-create-dialog-update"
                >
                  {t('llm_provider_update_button')}
                </LoadingButton>
              </DialogActions>
            </>
          );
        }}
      </Formik>
    </Dialog>
  );
};
