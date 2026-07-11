import { useState } from 'react';

import { useApiMutation } from 'tg.service/http/useQueryApi';
import { useProject } from 'tg.hooks/useProject';

import { useTranslationsSelector } from 'tg.views/projects/translations/context/TranslationsContext';
import { OperationProps } from 'tg.views/projects/translations/BatchOperations/types';
import { BatchOperationsSubmit } from 'tg.views/projects/translations/BatchOperations/components/BatchOperationsSubmit';
import { OperationContainer } from 'tg.views/projects/translations/BatchOperations/components/OperationContainer';
import { BatchOperationsLanguagesSelect } from 'tg.views/projects/translations/BatchOperations/components/BatchOperationsLanguagesSelect';

type Props = OperationProps;

export const OperationQaRecheck = ({ disabled, onStart }: Props) => {
  const project = useProject();
  const allLanguages = useTranslationsSelector((c) => c.languages) || [];
  const selection = useTranslationsSelector((c) => c.selection);

  const [selectedLangs, setSelectedLangs] = useState<string[]>([]);

  const batchLoadable = useApiMutation({
    url: '/v2/projects/{projectId}/start-batch-job/qa-check',
    method: 'post',
  });

  function handleSubmit() {
    const languageIds =
      selectedLangs.length > 0
        ? allLanguages
            ?.filter((l) => selectedLangs?.includes(l.tag))
            .map((l) => l.id)
        : undefined;

    batchLoadable.mutate(
      {
        path: { projectId: project.id },
        content: {
          'application/json': {
            keyIds: selection,
            languageIds,
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
        languages={allLanguages || []}
        value={selectedLangs || []}
        onChange={setSelectedLangs}
        languagePermission="translations.edit"
      />
      <BatchOperationsSubmit
        loading={batchLoadable.isLoading}
        disabled={disabled}
        onClick={handleSubmit}
      />
    </OperationContainer>
  );
};
