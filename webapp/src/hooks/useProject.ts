import { useContext } from 'react';
import { GlobalError } from '../error/GlobalError';
import { components } from '../service/apiSchema.generated';
import { ProjectContext } from './ProjectProvider';

export const useProject = (): components['schemas']['ProjectModel'] => {
  const projectDTOLoadable = useContext(ProjectContext);

  if (!projectDTOLoadable) {
    throw new GlobalError(
      'Unexpected error',
      'No data in loadable? Did you use provider before using hook?'
    );
  }

  return projectDTOLoadable;
};
