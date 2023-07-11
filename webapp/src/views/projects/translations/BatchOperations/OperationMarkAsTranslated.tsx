import { useState } from 'react';
import { ChevronRight } from '@mui/icons-material';
import { Box } from '@mui/material';

import { LanguagesSelect } from 'tg.component/common/form/LanguagesSelect/LanguagesSelect';
import LoadingButton from 'tg.component/common/form/LoadingButton';

import { useTranslationsSelector } from '../context/TranslationsContext';
import { OperationProps } from './types';
import { useApiMutation } from 'tg.service/http/useQueryApi';
import { useProject } from 'tg.hooks/useProject';
import { useTranslate } from '@tolgee/react';

type Props = OperationProps;

export const OperationMarkAsTranslated = ({ disabled, onStart }: Props) => {
  const { t } = useTranslate();
  const project = useProject();
  const allLanguages = useTranslationsSelector((c) => c.languages) || [];
  const selection = useTranslationsSelector((c) => c.selection);

  const [selectedLangs, setSelectedLangs] = useState<string[]>([]);

  const batchLoadable = useApiMutation({
    url: '/v2/projects/{projectId}/start-batch-job/set-translation-state',
    method: 'post',
  });

  function handleSubmit() {
    batchLoadable.mutate(
      {
        path: { projectId: project.id },
        content: {
          'application/json': {
            keyIds: selection,
            languageIds: allLanguages
              ?.filter((l) => selectedLangs?.includes(l.tag))
              .map((l) => l.id),
            state: 'TRANSLATED',
          },
        },
      },
      {
        onSuccess(data) {
          onStart(data);
        },
      }
    );
  }

  return (
    <Box display="flex" gap="10px">
      <LanguagesSelect
        languages={allLanguages || []}
        value={selectedLangs || []}
        onChange={setSelectedLangs}
        enableEmpty
        context="batch-operations"
        placeholder={t('batch_operations_select_languages_placeholder')}
      />
      <LoadingButton
        data-cy="batch-operations-submit-button"
        loading={batchLoadable.isLoading}
        disabled={disabled}
        sx={{ minWidth: 0, minHeight: 0, width: 40, height: 40 }}
        onClick={handleSubmit}
        variant="contained"
        color="primary"
      >
        <ChevronRight />
      </LoadingButton>
    </Box>
  );
};
