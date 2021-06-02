import {useSelector} from 'react-redux';
import {AppState} from '../store';
import {GlobalError} from '../error/GlobalError';
import {components} from '../service/apiSchema';

export const useProject = (): components['schemas']['ProjectModel'] => {
  let projectDTOLoadable = useSelector(
    (state: AppState) => state.projects.loadables.project
  );

  if (!projectDTOLoadable.data) {
    throw new GlobalError(
      'Unexpected error',
      'No data in loadable? Did you use provider before using hook?'
    );
  }

  return projectDTOLoadable.data;
};
