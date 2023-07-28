import { useState } from 'react';
import { useTranslate } from '@tolgee/react';

import { LanguagesSelect } from 'tg.component/common/form/LanguagesSelect/LanguagesSelect';
import { useApiMutation } from 'tg.service/http/useQueryApi';
import { useProject } from 'tg.hooks/useProject';

import { useTranslationsSelector } from '../context/TranslationsContext';
import { OperationProps } from './types';
import { BatchOperationsSubmit } from './components/BatchOperationsSubmit';
import { OperationContainer } from './components/OperationContainer';

type Props = OperationProps;

export const OperationClearTranslations = ({ disabled, onStart }: Props) => {
  const project = useProject();
  const { t } = useTranslate();
  const allLanguages = useTranslationsSelector((c) => c.languages) || [];
  const selection = useTranslationsSelector((c) => c.selection);

  const [selectedLangs, setSelectedLangs] = useState<string[]>([]);

  const batchLoadable = useApiMutation({
    url: '/v2/projects/{projectId}/start-batch-job/clear-translations',
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
      <LanguagesSelect
        languages={allLanguages || []}
        value={selectedLangs || []}
        onChange={setSelectedLangs}
        enableEmpty
        context="batch-operations"
        placeholder={t('batch_operations_select_languages_placeholder')}
      />
      <BatchOperationsSubmit
        loading={batchLoadable.isLoading}
        disabled={disabled || selectedLangs.length === 0}
        onClick={handleSubmit}
      />
    </OperationContainer>
  );
};
