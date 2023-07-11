import { useState } from 'react';
import { ChevronRight } from '@mui/icons-material';
import { Box, Button } from '@mui/material';

import { LanguagesSelect } from 'tg.component/common/form/LanguagesSelect/LanguagesSelect';
import { useTranslationsSelector } from '../context/TranslationsContext';
import { OperationProps } from './types';
import { useApiMutation } from 'tg.service/http/useQueryApi';
import { useProject } from 'tg.hooks/useProject';

type Props = OperationProps & {
  disabled: boolean;
};

export const OperationTranslate = ({ disabled }: Props) => {
  const project = useProject();
  const allLanguages = useTranslationsSelector((c) => c.languages) || [];
  const selectedGloballyLanguages = useTranslationsSelector(
    (c) => c.selectedLanguages
  );
  const selection = useTranslationsSelector((c) => c.selection);

  const languages = allLanguages.filter((l) => !l.base);
  const baseLang = allLanguages.find((l) => l.base);

  const [selectedLangs, setSelectedLangs] = useState(
    selectedGloballyLanguages?.filter((tag) => tag !== baseLang?.tag)
  );

  const batchTranslate = useApiMutation({
    url: '/v2/projects/{projectId}/start-batch-job/translate',
    method: 'put',
  });

  function handleSubmit() {
    batchTranslate.mutate({
      path: { projectId: project.id },
      content: {
        'application/json': {
          keyIds: selection,
          targetLanguageIds: allLanguages
            ?.filter((l) => selectedLangs?.includes(l.tag))
            .map((l) => l.id),
          useMachineTranslation: true,
          useTranslationMemory: true,
          service: undefined,
        },
      },
    });
  }

  return (
    <Box display="flex" gap="10px">
      <LanguagesSelect
        languages={languages || []}
        value={selectedLangs || []}
        onChange={setSelectedLangs}
        enableEmpty
        context="batch-operations"
      />
      <Button
        data-cy="batch-operations-translate-button"
        disabled={disabled || !selectedLangs?.length}
        sx={{ minWidth: 0, minHeight: 0, width: 40, height: 40 }}
        onClick={handleSubmit}
        variant="contained"
        color="primary"
      >
        <ChevronRight />
      </Button>
    </Box>
  );
};
