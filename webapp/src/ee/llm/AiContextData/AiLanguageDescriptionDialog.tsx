import {
  Box,
  Button,
  Dialog,
  DialogActions,
  DialogContent,
  TextField,
  Typography,
} from '@mui/material';
import { useTranslate } from '@tolgee/react';
import { useState } from 'react';
import LoadingButton from 'tg.component/common/form/LoadingButton';
import { confirmDiscardUnsaved } from 'tg.hooks/confirmation';
import { useProject } from 'tg.hooks/useProject';
import { components } from 'tg.service/apiSchema.generated';
import { useApiMutation } from 'tg.service/http/useQueryApi';
import { LanguageItem } from 'tg.component/languages/LanguageItem';
import { AiTips } from './AiTips';

type LanguageModel = components['schemas']['LanguageModel'];

type Props = {
  placeholder: string;
  onClose: () => void;
  currentValue: string;
  language: LanguageModel;
};

export const AiLanguageDescriptionDialog = ({
  onClose,
  placeholder,
  currentValue,
  language,
}: Props) => {
  const project = useProject();
  const { t } = useTranslate();

  const [inputValue, setInputValue] = useState(currentValue);

  const saveDescription = useApiMutation({
    url: '/v2/projects/{projectId}/languages/{languageId}/ai-prompt-customization',
    method: 'put',
    invalidatePrefix:
      '/v2/projects/{projectId}/language-ai-prompt-customizations',
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
        path: { projectId: project.id, languageId: language.id },
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
      <Box sx={{ mt: 2, mx: 3, display: 'grid', gap: 0.5 }}>
        <Typography variant="h6">
          {t('language_ai_prompt_dialog_title')}
        </Typography>
        <LanguageItem language={language} />
      </Box>
      <DialogContent>
        <Box sx={{ display: 'grid', gap: '16px' }}>
          <TextField
            multiline
            sx={{ width: '100%', mt: '2px' }}
            placeholder={placeholder}
            minRows={2}
            value={inputValue}
            onChange={(e) => setInputValue(e.currentTarget.value)}
            error={isTooLong}
            helperText={
              isTooLong && t('language_ai_prompt_dialog_description_too_long')
            }
            data-cy="language-ai-prompt-dialog-description-input"
          />
          <AiTips
            tips={[
              t('language_ai_prompt_tip_usage'),
              t('language_ai_prompt_tip_language'),
            ]}
          />
        </Box>
      </DialogContent>
      <DialogActions>
        <Button onClick={handleClose}>{t('global_cancel_button')}</Button>
        <LoadingButton
          onClick={handleSave}
          loading={saveDescription.isLoading}
          color="primary"
          variant="contained"
          disabled={isTooLong}
          data-cy="language-ai-prompt-dialog-save"
        >
          {t('global_form_save')}
        </LoadingButton>
      </DialogActions>
    </Dialog>
  );
};
