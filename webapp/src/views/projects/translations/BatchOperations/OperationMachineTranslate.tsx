import { useState } from 'react';

import { useApiMutation, useApiQuery } from 'tg.service/http/useQueryApi';
import { useProject } from 'tg.hooks/useProject';

import { useTranslationsSelector } from '../context/TranslationsContext';
import { OperationProps } from './types';
import { BatchOperationsSubmit } from './components/BatchOperationsSubmit';
import { OperationContainer } from './components/OperationContainer';
import { BatchOperationsLanguagesSelect } from './components/BatchOperationsLanguagesSelect';
import { BatchModeSelector } from './components/BatchModeSelector';
import { getPreselectedLanguages } from './getPreselectedLanguages';

type Props = OperationProps;

export const OperationMachineTranslate = ({ disabled, onStart }: Props) => {
  const project = useProject();
  const allLanguages = useTranslationsSelector((c) => c.languages) || [];
  const translationsLanguages = useTranslationsSelector(
    (c) => c.translationsLanguages
  );
  const selection = useTranslationsSelector((c) => c.selection);

  const languages = allLanguages.filter((l) => !l.base);

  const [selectedLangs, setSelectedLangs] = useState<string[]>(() =>
    getPreselectedLanguages(languages, translationsLanguages ?? [])
  );

  const [mode, setMode] = useState<'instant' | 'batch'>('instant');

  const selectedLangIds = allLanguages
    ?.filter((l) => selectedLangs?.includes(l.tag))
    .map((l) => l.id);

  const batchInfoLoadable = useApiQuery({
    url: '/v2/projects/{projectId}/batch-translate-info',
    method: 'get',
    path: { projectId: project.id },
    query: { targetLanguageIds: selectedLangIds.join(',') },
    options: {
      enabled: selectedLangIds.length > 0,
      staleTime: 30_000,
      retry: false,
    },
  });

  const batchInfo = batchInfoLoadable.data;
  const showModeSelector =
    batchInfo?.available && batchInfo?.userChoiceAllowed;

  const batchLoadable = useApiMutation({
    url: '/v2/projects/{projectId}/start-batch-job/machine-translate',
    method: 'post',
  });

  function handleSubmit() {
    batchLoadable.mutate(
      {
        path: { projectId: project.id },
        content: {
          'application/json': {
            keyIds: selection,
            targetLanguageIds: selectedLangIds,
            useBatchApi: mode === 'batch' ? true : undefined,
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
    <OperationContainer>
      <BatchOperationsLanguagesSelect
        languages={languages || []}
        value={selectedLangs || []}
        onChange={setSelectedLangs}
        languagePermission="translations.edit"
      />
      {showModeSelector && (
        <BatchModeSelector
          batchInfo={batchInfo}
          isLoading={batchInfoLoadable.isLoading}
          value={mode}
          onChange={setMode}
        />
      )}
      <BatchOperationsSubmit
        loading={batchLoadable.isLoading}
        disabled={disabled || selectedLangs.length === 0}
        onClick={handleSubmit}
      />
    </OperationContainer>
  );
};
