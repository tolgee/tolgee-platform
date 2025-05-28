import {
  Button,
  Dialog,
  DialogActions,
  DialogContent,
  DialogTitle,
  FormControlLabel,
} from '@mui/material';
import { T, useTranslate } from '@tolgee/react';
import { Formik } from 'formik';

import { components } from 'tg.service/apiSchema.generated';
import { useApiMutation } from 'tg.service/http/useQueryApi';
import { messageService } from 'tg.service/MessageService';
import LoadingButton from 'tg.component/common/form/LoadingButton';
import { TextField } from 'tg.component/common/form/fields/TextField';
import { Validation } from 'tg.constants/GlobalValidationSchema';
import { Checkbox } from 'tg.component/common/form/fields/Checkbox';
import { useProjectPermissions } from 'tg.hooks/useProjectPermissions';
import { PromptItem } from './PromptLoadMenu';

type PromptModel = components['schemas']['PromptModel'];

type Props = {
  projectId: number;
  onClose: () => void;
  data: Omit<PromptModel, 'projectId' | 'id' | 'name'>;
  onSuccess: (prompt: PromptItem) => void;
};

export const PromptSaveDialog = ({
  projectId,
  onClose,
  data,
  onSuccess,
}: Props) => {
  const { t } = useTranslate();
  const { satisfiesPermission } = useProjectPermissions();

  const createPrompt = useApiMutation({
    url: '/v2/projects/{projectId}/prompts',
    method: 'post',
    invalidatePrefix: '/v2/projects/{projectId}/prompts',
  });

  const setPromptAsDefault = useApiMutation({
    url: '/v2/projects/{projectId}/machine-translation-service-settings/set-default-prompt/{promptId}',
    method: 'put',
    invalidatePrefix:
      '/v2/projects/{projectId}/machine-translation-service-settings',
  });

  return (
    <Dialog open={true} onClose={onClose}>
      <Formik
        initialValues={{ name: '', useAsDefault: false }}
        validationSchema={Validation.PROMPT_SAVE_AS()}
        onSubmit={async ({ name, useAsDefault }) => {
          const result = await createPrompt.mutateAsync({
            path: { projectId },
            content: { 'application/json': { ...data, name } },
          });
          if (useAsDefault) {
            await setPromptAsDefault.mutateAsync({
              path: { projectId, promptId: result.id },
            });
          }
          onClose();
          messageService.success(<T keyName="ai_prompt_create_success" />);
          onSuccess(result);
        }}
      >
        {({ handleSubmit }) => (
          <>
            <DialogTitle>{t('ai_prompt_save_as_title')}</DialogTitle>
            <DialogContent sx={{ minWidth: 300 }}>
              <TextField
                name="name"
                label={t('ai_prompt_save_as_field_name')}
                data-cy="ai-prompt-save-as-field-name"
              />
              {satisfiesPermission('languages.edit') && (
                <FormControlLabel
                  control={<Checkbox name="useAsDefault" size="small" />}
                  label={t('ai_prompt_save_as_field_default')}
                  data-cy="ai-prompt-save-as-field-use-as-default"
                />
              )}
            </DialogContent>
            <DialogActions>
              <Button onClick={onClose} data-cy="ai-prompt-save-dialog-cancel">
                {t('global_cancel_button')}
              </Button>
              <LoadingButton
                loading={createPrompt.isLoading}
                onClick={() => handleSubmit()}
                variant="contained"
                color="primary"
                data-cy="ai-prompt-save-dialog-save"
              >
                {t('global_form_save')}
              </LoadingButton>
            </DialogActions>
          </>
        )}
      </Formik>
    </Dialog>
  );
};
