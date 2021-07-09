import { container } from 'tsyringe';

import { startLoading } from 'tg.hooks/loading';
import { useProject } from 'tg.hooks/useProject';
import { ImportActions } from 'tg.store/project/ImportActions';

const actions = container.resolve(ImportActions);

export const useImportDataHelper = () => {
  const project = useProject();
  const result = actions.useSelector((s) => s.result);

  const onNewFiles = async (files: File[]) => {
    startLoading();
    actions.loadableActions.addFiles.dispatch({
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
    startLoading();
    actions.loadableActions.getResult.dispatch({
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
    actions.resetResult.dispatch();
    actions.loadableReset.getResult.dispatch();
    actions.loadableReset.addFiles.dispatch();
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
