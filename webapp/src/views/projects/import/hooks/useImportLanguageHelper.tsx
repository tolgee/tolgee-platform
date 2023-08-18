import { useImportDataHelper } from './useImportDataHelper';
import { confirmation } from 'tg.hooks/confirmation';
import { T } from '@tolgee/react';
import { useApiMutation } from 'tg.service/http/useQueryApi';
import { useProject } from 'tg.hooks/useProject';
import { components } from 'tg.service/apiSchema.generated';

export const useImportLanguageHelper = (
  row: components['schemas']['ImportLanguageModel']
) => {
  const project = useProject();
  const dataHelper = useImportDataHelper();

  const deleteMutation = useApiMutation({
    url: '/v2/projects/{projectId}/import/result/languages/{languageId}',
    method: 'delete',
    options: {
      onSuccess() {
        dataHelper.refetchData();
      },
    },
  });

  const selectExistingMutation = useApiMutation({
    url: '/v2/projects/{projectId}/import/result/languages/{importLanguageId}/select-existing/{existingLanguageId}',
    method: 'put',
    options: {
      onSuccess() {
        dataHelper.refetchData();
      },
    },
  });

  const resetExistingMutation = useApiMutation({
    url: '/v2/projects/{projectId}/import/result/languages/{importLanguageId}/reset-existing',
    method: 'put',
    options: {
      onSuccess() {
        dataHelper.refetchData();
      },
    },
  });

  const onDelete = () => {
    confirmation({
      onConfirm: () =>
        deleteMutation.mutate({
          path: {
            projectId: project.id,
            languageId: row.id,
          },
        }),

      title: <T keyName="import_delete_language_dialog_title" />,
      message: (
        <T
          keyName="import_delete_language_dialog_message"
          params={{ languageName: row.name }}
        />
      ),
    });
  };

  const onSelectExisting = (newLangId: number) => {
    selectExistingMutation.mutate({
      path: {
        projectId: project.id,
        importLanguageId: row.id,
        existingLanguageId: newLangId,
      },
    });
  };
  const onResetExisting = () => {
    resetExistingMutation.mutate({
      path: {
        projectId: project.id,
        importLanguageId: row.id,
      },
    });
  };

  return {
    onDelete,
    onSelectExisting,
    onResetExisting,
    mutations: {
      selectExistingLanguage: selectExistingMutation,
      languageDeleteMutation: deleteMutation,
    },
  };
};
