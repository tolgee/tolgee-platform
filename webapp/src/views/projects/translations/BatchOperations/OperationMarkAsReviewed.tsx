import { useState } from 'react';
import { useTranslate } from '@tolgee/react';

import { LanguagesSelect } from 'tg.component/common/form/LanguagesSelect/LanguagesSelect';
import { useProject } from 'tg.hooks/useProject';
import { useApiMutation } from 'tg.service/http/useQueryApi';

import { useTranslationsSelector } from '../context/TranslationsContext';
import { OperationProps } from './types';
import { BatchOperationsSubmit } from './components/BatchOperationsSubmit';
import { OperationContainer } from './components/OperationContainer';

type Props = OperationProps;

export const OperationMarkAsReviewed = ({ disabled, onStart }: Props) => {
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
            state: 'REVIEWED',
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
