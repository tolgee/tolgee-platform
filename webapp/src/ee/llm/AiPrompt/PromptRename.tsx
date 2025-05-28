import {
  Button,
  Dialog,
  DialogActions,
  DialogContent,
  DialogTitle,
} from '@mui/material';
import { useTranslate } from '@tolgee/react';
import { Formik } from 'formik';

import { components } from 'tg.service/apiSchema.generated';
import { TextField } from 'tg.component/common/form/fields/TextField';
import { useApiMutation } from 'tg.service/http/useQueryApi';
import LoadingButton from 'tg.component/common/form/LoadingButton';
import { Validation } from 'tg.constants/GlobalValidationSchema';

type PromptDto = components['schemas']['PromptDto'];

export type PromptItem = PromptDto & { id?: number };

type Props = {
  data: PromptItem;
  projectId: number;
  onClose: () => void;
};

export const PromptRename = ({ data, projectId, onClose }: Props) => {
  const { t } = useTranslate();

  const updateLoadable = useApiMutation({
    url: '/v2/projects/{projectId}/prompts/{promptId}',
    method: 'put',
    invalidatePrefix: '/v2/projects/{projectId}/prompts',
  });

  return (
    <Dialog open={true} onClose={onClose}>
      <Formik
        initialValues={{ name: data.name }}
        validationSchema={Validation.PROMPT_RENAME()}
        onSubmit={(values) => {
          updateLoadable.mutate(
            {
              path: { projectId, promptId: data.id! },
              content: {
                'application/json': { ...data, name: values.name },
              },
            },
            {
              onSuccess() {
                onClose();
              },
            }
          );
        }}
      >
        {({ handleSubmit, values }) => (
          <>
            <DialogTitle>{t('ai_prompt_rename_title')}</DialogTitle>
            <DialogContent sx={{ minWidth: 300 }}>
              <TextField
                name="name"
                fullWidth
                label={t('ai_prompt_rename_name_field')}
                data-cy="ai-prompt-rename-name-field"
              />
            </DialogContent>
            <DialogActions>
              <Button onClick={onClose} data-cy="ai-prompt-rename-cancel">
                {t('global_cancel_button')}
              </Button>
              <LoadingButton
                loading={updateLoadable.isLoading}
                disabled={values.name === data.name}
                onClick={() => handleSubmit()}
                variant="contained"
                color="primary"
                data-cy="ai-prompt-rename-save"
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
