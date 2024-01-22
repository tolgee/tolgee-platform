import {
  Button,
  Dialog,
  DialogActions,
  DialogContent,
  DialogTitle,
  TextField,
} from '@mui/material';
import { useTranslate } from '@tolgee/react';
import { useState } from 'react';
import LoadingButton from 'tg.component/common/form/LoadingButton';
import { confirmDiscardUnsaved } from 'tg.hooks/confirmation';
import { useProject } from 'tg.hooks/useProject';
import { useApiMutation } from 'tg.service/http/useQueryApi';
import { AiTips } from './AiTips';

type Props = {
  placeholder: string;
  onClose: () => void;
  currentValue: string;
};

export const AiProjectDescriptionDialog = ({
  onClose,
  placeholder,
  currentValue,
}: Props) => {
  const project = useProject();
  const { t } = useTranslate();

  const [inputValue, setInputValue] = useState(currentValue);

  const saveDescription = useApiMutation({
    url: '/v2/projects/{projectId}/ai-prompt-customization',
    method: 'put',
    invalidatePrefix: '/v2/projects/{projectId}/ai-prompt-customization',
  });

  function handleClose() {
    if (inputValue !== currentValue) {
      confirmDiscardUnsaved({
        onConfirm() {
          onClose();
        },
      });
    } else {
      onClose();
    }
  }

  function handleSave() {
    saveDescription.mutate(
      {
        path: { projectId: project.id },
        content: { 'application/json': { description: inputValue } },
      },
      {
        onSuccess() {
          onClose();
        },
      }
    );
  }

  const isTooLong = inputValue?.length > 2000;

  return (
    <Dialog open fullWidth maxWidth="sm" onClose={handleClose}>
      <DialogTitle>{t('project_ai_prompt_dialog_title')}</DialogTitle>
      <DialogContent sx={{ display: 'grid', gap: '16px' }}>
        <TextField
          multiline
          sx={{ width: '100%', mt: '2px' }}
          placeholder={placeholder}
          minRows={2}
          value={inputValue}
          onChange={(e) => setInputValue(e.currentTarget.value)}
          error={isTooLong}
          helperText={
            isTooLong && t('project_ai_prompt_dialog_description_too_long')
          }
          data-cy="project-ai-prompt-dialog-description-input"
        />
        <AiTips
          tips={[
            t('project_ai_prompt_dialog_tip_topic'),
            t('project_ai_prompt_dialog_tip_language'),
          ]}
        />
      </DialogContent>
      <DialogActions>
        <Button onClick={handleClose}>{t('global_cancel_button')}</Button>
        <LoadingButton
          onClick={handleSave}
          loading={saveDescription.isLoading}
          color="primary"
          variant="contained"
          disabled={isTooLong}
          data-cy="project-ai-prompt-dialog-save"
        >
          {t('global_form_save')}
        </LoadingButton>
      </DialogActions>
    </Dialog>
  );
};
