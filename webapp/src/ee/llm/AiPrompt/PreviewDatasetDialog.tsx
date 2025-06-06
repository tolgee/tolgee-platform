import {
  Alert,
  AlertTitle,
  Box,
  Button,
  Chip,
  Dialog,
  DialogActions,
  DialogContent,
  DialogTitle,
  styled,
} from '@mui/material';
import { T, useTranslate } from '@tolgee/react';
import { useApiMutation, useApiQuery } from 'tg.service/http/useQueryApi';
import { BatchJobModel } from 'tg.views/projects/translations/BatchOperations/types';
import { BasicPromptOption } from './TabBasic';
import { BoxLoading } from 'tg.component/common/BoxLoading';
import { Lightbulb02 } from '@untitled-ui/icons-react';
import { BatchOperationsLanguagesSelect } from 'tg.views/projects/translations/BatchOperations/components/BatchOperationsLanguagesSelect';
import {
  useTranslationsActions,
  useTranslationsSelector,
} from 'tg.views/projects/translations/context/TranslationsContext';
import { useState } from 'react';
import { getPreselectedLanguages } from 'tg.views/projects/translations/BatchOperations/getPreselectedLanguages';

export const AI_PLAYGROUND_TAG = 'ai-playground';

const StyledList = styled('ol')`
  padding-left: 20px;
`;

type Props = {
  onClose: () => void;
  projectId: number;
  onStart: (data: BatchJobModel) => void;
  providerName: string;
  template: string | undefined;
  options: BasicPromptOption[] | undefined;
};

export const PreviewDatasetDialog = ({
  onClose,
  projectId,
  template,
  providerName,
  onStart,
  options,
}: Props) => {
  const { setFilters } = useTranslationsActions();
  const { t } = useTranslate();
  const keysLoadable = useApiQuery({
    url: '/v2/projects/{projectId}/keys/select',
    method: 'get',
    query: {
      filterTag: ['ai-playground'],
    },
    path: {
      projectId,
    },
  });

  const taggedKeys = keysLoadable.data?.ids;

  const allLanguages = useTranslationsSelector((c) => c.languages) || [];
  const translationsLanguages = useTranslationsSelector(
    (c) => c.translationsLanguages
  );
  const languages = allLanguages.filter((l) => !l.base);

  const [selectedLangs, setSelectedLangs] = useState<string[]>(() =>
    getPreselectedLanguages(languages, translationsLanguages ?? [])
  );

  const mtTranslate = useApiMutation({
    url: '/v2/projects/{projectId}/start-batch-job/ai-playground-translate',
    method: 'post',
  });

  const handleRunBatch = async () => {
    mtTranslate
      .mutateAsync({
        content: {
          'application/json': {
            keyIds: taggedKeys ?? [],
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
        setFilters({ filterTag: [AI_PLAYGROUND_TAG] });
        onClose();
      });
  };

  return (
    <Dialog onClose={onClose} open={true}>
      <DialogTitle>
        {taggedKeys?.length ? (
          <T
            keyName="ai_prompt_dataset_title_keys"
            params={{ value: taggedKeys.length }}
          />
        ) : (
          <T keyName="ai_prompt_dataset_title_no_keys" />
        )}
      </DialogTitle>
      <DialogContent sx={{ display: 'grid', gap: 2, minWidth: 400 }}>
        {keysLoadable.isLoading ? (
          <BoxLoading />
        ) : taggedKeys?.length ? (
          <>
            <BatchOperationsLanguagesSelect
              languages={allLanguages || []}
              value={selectedLangs || []}
              onChange={setSelectedLangs}
              languagePermission="translations.view"
            />
            <Box>
              <T
                keyName="ai_prompt_dataset_tags"
                params={{
                  highlight: (value) => <Chip label={value} size="small" />,
                  tagName: AI_PLAYGROUND_TAG,
                }}
              />
            </Box>
          </>
        ) : (
          <>
            <Box data-cy="ai-prompt-dataset-no-tags-text">
              <T
                keyName="ai_prompt_dataset_no_tags"
                params={{
                  highlight: (value) => <Chip label={value} size="small" />,
                  tagName: AI_PLAYGROUND_TAG,
                }}
              />
            </Box>
            <Alert severity="info" icon={<Lightbulb02 />}>
              <AlertTitle>{t('ai_prompt_dataset_guide_title')}</AlertTitle>
              <StyledList>
                <T
                  keyName="ai_prompt_dataset_guide_points"
                  params={{
                    li: <li />,
                    tagName: AI_PLAYGROUND_TAG,
                    operationName: t('batch_operations_add_tags'),
                  }}
                />
              </StyledList>
            </Alert>
          </>
        )}
      </DialogContent>
      <DialogActions>
        {keysLoadable.isLoading ? null : taggedKeys?.length ? (
          <>
            <Button onClick={onClose} data-cy="ai-prompt-dataset-cancel">
              {t('global_cancel_button')}
            </Button>
            <Button
              onClick={handleRunBatch}
              color="primary"
              data-cy="ai-prompt-dataset-run"
            >
              {t('ai_prompt_dataset_run')}
            </Button>
          </>
        ) : (
          <Button
            color="primary"
            onClick={onClose}
            data-cy="ai-prompt-dataset-got-it"
          >
            {t('ai_prompt_dataset_got_it')}
          </Button>
        )}
      </DialogActions>
    </Dialog>
  );
};
