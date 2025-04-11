import {
  Dialog,
  DialogActions,
  DialogContent,
  DialogTitle,
} from '@mui/material';
import { Formik } from 'formik';
import { components } from 'tg.service/apiSchema.generated';
import { LLMProviderForm } from './LLMProviderForm';
import { T, useTranslate } from '@tolgee/react';
import { LoadingButton } from '@mui/lab';
import { useApiMutation } from 'tg.service/http/useQueryApi';
import { useOrganization } from '../useOrganization';
import { messageService } from 'tg.service/MessageService';

type LLMProviderRequest = components['schemas']['LLMProviderRequest'];

type Props = {
  onClose: () => void;
};

export const LLMProviderCreateDialog = ({ onClose }: Props) => {
  const { t } = useTranslate();
  const organization = useOrganization();

  const createLoadable = useApiMutation({
    url: '/v2/organizations/{organizationId}/llm-providers',
    method: 'post',
    invalidatePrefix: '/v2/organizations/{organizationId}/llm-providers',
  });

  return (
    <Dialog open={true} onClose={onClose}>
      <Formik
        initialValues={
          {
            name: '',
            type: 'OPENAI',
          } as LLMProviderRequest
        }
        onSubmit={(values) => {
          createLoadable.mutate(
            {
              content: { 'application/json': values },
              path: {
                organizationId: organization!.id,
              },
            },
            {
              onSuccess() {
                messageService.success(
                  <T keyName="llm_provider_create_success_message" />
                );
                onClose();
              },
            }
          );
        }}
      >
        {({ submitForm }) => {
          return (
            <>
              <DialogTitle>{t('llm_provider_create_title')}</DialogTitle>
              <DialogContent
                sx={{
                  display: 'grid',
                  gridTemplateColumns: '1fr 1fr',
                  gap: '0px 16px',
                }}
              >
                <LLMProviderForm />
              </DialogContent>
              <DialogActions>
                <LoadingButton
                  onClick={submitForm}
                  loading={createLoadable.isLoading}
                >
                  {t('global_form_save')}
                </LoadingButton>
              </DialogActions>
            </>
          );
        }}
      </Formik>
    </Dialog>
  );
};
