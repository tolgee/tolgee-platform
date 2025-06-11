import {
  Button,
  Dialog,
  DialogActions,
  DialogContent,
  DialogTitle,
  Alert,
  Chip,
  Link,
} from '@mui/material';
import { T, useTranslate } from '@tolgee/react';
import { useState } from 'react';
import { components } from 'tg.service/apiSchema.generated';
import { useApiMutation } from 'tg.service/http/useQueryApi';
import { BatchOperationsLanguagesSelect } from 'tg.views/projects/translations/BatchOperations/components/BatchOperationsLanguagesSelect';
import { getPreselectedLanguages } from 'tg.views/projects/translations/BatchOperations/getPreselectedLanguages';
import {
  useTranslationsActions,
  useTranslationsSelector,
} from 'tg.views/projects/translations/context/TranslationsContext';
import { BasicPromptOption } from './TabBasic';
import { AI_PLAYGROUND_TAG } from './PreviewDatasetDialog';
import { DOCS_ROOT } from 'tg.constants/docLinks';

type BatchJobModel = components['schemas']['BatchJobModel'];

type Props = {
  onStart: (data: BatchJobModel) => void;
  onClose: () => void;
  providerName: string;
  template: string | undefined;
  options: BasicPromptOption[] | undefined;
  projectId: number;
  numberOfKeys: number;
};

export const PreviewBatchDialog = ({
  onStart,
  onClose,
  providerName,
  template,
  options,
  projectId,
  numberOfKeys,
}: Props) => {
  const { getAllIds } = useTranslationsActions();
  const { t } = useTranslate();

  const mtTranslate = useApiMutation({
    url: '/v2/projects/{projectId}/start-batch-job/ai-playground-translate',
    method: 'post',
  });

  const allLanguages = useTranslationsSelector((c) => c.languages) || [];
  const translationsLanguages = useTranslationsSelector(
    (c) => c.translationsLanguages
  );
  const languages = allLanguages.filter((l) => !l.base);

  const [selectedLangs, setSelectedLangs] = useState<string[]>(() =>
    getPreselectedLanguages(languages, translationsLanguages ?? [])
  );

  const handleRunBatch = async () => {
    const allIds = await getAllIds();
    mtTranslate
      .mutateAsync({
        content: {
          'application/json': {
            keyIds: allIds,
            targetLanguageIds: allLanguages
              ?.filter((l) => selectedLangs?.includes(l.tag))
              .map((l) => l.id),
            llmPrompt: {
              name: '',
              template,
              basicPromptOptions: options,
              providerName,
            },
          },
        },
        path: {
          projectId,
        },
      })
      .then((data) => {
        onStart(data);
        onClose();
      });
  };

  return (
    <Dialog open={true} onClose={onClose}>
      <DialogTitle>
        {t('ai_prompt_batch_dialog_title', { value: numberOfKeys })}
      </DialogTitle>
      <DialogContent sx={{ display: 'grid', gap: 2 }}>
        <BatchOperationsLanguagesSelect
          languages={allLanguages || []}
          value={selectedLangs || []}
          onChange={setSelectedLangs}
          languagePermission="translations.view"
        />
        {numberOfKeys > 10 && (
          <Alert color="warning" icon={false}>
            <T
              keyName="ai_prompt_batch_dialog_many_keys_warning"
              params={{
                highlight: (value) => <Chip label={value} size="small" />,
                tagName: AI_PLAYGROUND_TAG,
                link: (
                  <Link
                    href={`${DOCS_ROOT}/platform/translation_process/ai-playground#preview-of-selected-dataset`}
                    target="_blank"
                  />
                ),
              }}
            />
          </Alert>
        )}
      </DialogContent>
      <DialogActions>
        <Button onClick={onClose} data-cy="ai-prompt-batch-dialog-cancel">
          {t('global_cancel_button')}
        </Button>
        <Button
          onClick={handleRunBatch}
          color="primary"
          data-cy="ai-prompt-batch-dialog-run"
        >
          {t('ai_prompt_batch_dialog_run')}
        </Button>
      </DialogActions>
    </Dialog>
  );
};
