import { useProject } from 'tg.hooks/useProject';
import { useGlobalLoading } from 'tg.component/GlobalLoading';
import create from 'zustand';
import { components } from 'tg.service/apiSchema.generated';
import { useApiMutation, useApiQuery } from 'tg.service/http/useQueryApi';
import { T } from '@tolgee/react';
import { useMessage } from 'tg.hooks/useSuccessMessage';
import { useEffect } from 'react';

type ResultType = components['schemas']['PagedModelImportLanguageModel'];

const useImportDataStore = create<{
  result: ResultType | undefined;
  setResult: (result?: ResultType, projectId?: number) => void;
  applyTouched: boolean;
  // to control whether we have data of current project in this global store
  projectId?: number;
  setApplyTouched: (value: boolean) => void;
}>((set) => ({
  result: undefined,
  setApplyTouched: (value: boolean) => set({ applyTouched: value }),
  setResult(result?: ResultType, projectId?: number) {
    set(() => ({ result, projectId }));
  },
  applyTouched: false,
}));

export const useImportDataHelper = () => {
  const project = useProject();
  const result = useImportDataStore((s) => s.result);
  const resultProjectId = useImportDataStore((s) => s.projectId);
  const setResult = useImportDataStore((s) => s.setResult);
  const setApplyTouched = useImportDataStore((s) => s.setApplyTouched);
  const applyTouched = useImportDataStore((s) => s.applyTouched);

  const message = useMessage();

  const resultLoadable = useApiQuery({
    url: '/v2/projects/{projectId}/import/result',
    method: 'get',
    path: {
      projectId: project.id,
    },
    fetchOptions: {
      disableNotFoundHandling: true,
    },
    query: { size: 1000 },
    options: {
      onSuccess(data) {
        setResult(data, project.id);
      },
      onError(error) {
        if ((error as any)?.code === 'resource_not_found') {
          setResult(undefined);
        }
      },
    },
  });

  useEffect(() => {
    if (project.id !== resultProjectId) {
      setResult(undefined);
      setApplyTouched(false);
      resultLoadable.refetch();
    }
  }, [project.id, resultProjectId]);

  const addFilesMutation = useApiMutation({
    url: '/v2/projects/{projectId}/import',
    method: 'post',
    options: {
      onSuccess(data) {
        setResult(data.result, project.id);
      },
      onError(error) {
        if (error?.code === 'cannot_add_more_then_100_languages') {
          message.error(
            <T params={{ n: '100' }}>
              import_error_cannot_add_more_then_n_languages
            </T>
          );
        }
      },
    },
    fetchOptions: {
      disableBadRequestHandling: true,
    },
  });

  const cancelMutation = useApiMutation({
    url: '/v2/projects/{projectId}/import',
    method: 'delete',
    options: {
      onSuccess(data) {
        setResult(undefined);
      },
    },
    fetchOptions: {
      disableBadRequestHandling: true,
    },
  });

  useGlobalLoading(addFilesMutation.isLoading || resultLoadable.isLoading);

  const onNewFiles = async (files: File[]) => {
    addFilesMutation.mutate({
      path: {
        projectId: project.id,
      },
      query: {},
      content: {
        'multipart/form-data': {
          files: files as any,
        },
      },
    });
  };

  const onCancel = async () =>
    cancelMutation.mutate({
      path: {
        projectId: project.id,
      },
    });

  useGlobalLoading(cancelMutation.isLoading);

  return {
    onNewFiles,
    result,
    resultLoadable: resultLoadable,
    addFilesMutation: addFilesMutation,
    cancelMutation,
    touchApply: () => setApplyTouched(true),
    applyTouched,
    onCancel,
    refetchData: () => resultLoadable.refetch({ cancelRefetch: true }),
    get isValid() {
      return !!result?._embedded?.languages?.every(
        (il) => il.existingLanguageId
      );
    },
  };
};
