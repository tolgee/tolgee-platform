import { useRepository } from '../../../../../hooks/useRepository';
import { container } from 'tsyringe';
import { startLoading } from '../../../../../hooks/loading';
import { ImportActions } from '../../../../../store/repository/ImportActions';

const actions = container.resolve(ImportActions);

export const useImportDataHelper = () => {
  const repository = useRepository();
  const result = actions.useSelector((s) => s.result);

  const onNewFiles = async (files: File[]) => {
    startLoading();
    actions.loadableActions.addFiles.dispatch({
      path: {
        repositoryId: repository.id,
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
        repositoryId: repository.id,
      },
      query: {
        pageable: { page: 0, size: 100 },
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
