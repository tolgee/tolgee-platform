import { useSelector } from 'react-redux';
import { AppState } from '../store';
import { GlobalError } from '../error/GlobalError';
import { components } from '../service/apiSchema';

export const useRepository = (): components['schemas']['RepositoryModel'] => {
  let repositoryDTOLoadable = useSelector(
    (state: AppState) => state.repositories.loadables.repository
  );

  if (!repositoryDTOLoadable.data) {
    throw new GlobalError(
      'Unexpected error',
      'No data in loadable? Did you use provider before using hook?'
    );
  }

  return repositoryDTOLoadable.data;
};
