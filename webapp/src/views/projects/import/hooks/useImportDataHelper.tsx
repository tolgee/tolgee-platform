import { useProject } from 'tg.hooks/useProject';
import { importActions } from 'tg.store/project/ImportActions';
import { useGlobalLoading } from 'tg.component/GlobalLoading';

export const useImportDataHelper = () => {
  const project = useProject();
  const result = importActions.useSelector((s) => s.result);
  const addFilesLoadable = importActions.useSelector(
    (s) => s.loadables.addFiles
  );
  const getResultLoadable = importActions.useSelector(
    (s) => s.loadables.getResult
  );

  useGlobalLoading(addFilesLoadable.loading || getResultLoadable.loading);

  const onNewFiles = async (files: File[]) => {
    importActions.loadableActions.addFiles.dispatch({
      path: {
        projectId: project.id,
      },
      content: {
        'multipart/form-data': {
          files: files as any,
        },
      },
    });
  };

  const loadData = () => {
    importActions.loadableActions.getResult.dispatch({
      path: {
        projectId: project.id,
      },
      query: {
        page: 0,
        size: 100,
      },
    });
  };

  const resetResult = () => {
    importActions.resetResult.dispatch();
    importActions.loadableReset.getResult.dispatch();
    importActions.loadableReset.addFiles.dispatch();
  };

  return {
    onNewFiles,
    result,
    loadData,
    resetResult,
    get isValid() {
      return !!result?._embedded?.languages?.every(
        (il) => il.existingLanguageId
      );
    },
  };
};
