import { createContext, FunctionComponent } from 'react';

import { GlobalError } from '../error/GlobalError';
import { components } from '../service/apiSchema.generated';
import { useApiQuery } from '../service/http/useQueryApi';
import { useProject } from './useProject';
import { FullPageLoading } from 'tg.component/common/FullPageLoading';

export const ProjectLanguagesContext =
  // @ts-ignore
  createContext<components['schemas']['PagedModelLanguageModel']>(null);

export const ProjectLanguagesProvider: FunctionComponent = (props) => {
  const projectDTO = useProject();

  const languagesLoadable = useApiQuery({
    url: '/v2/projects/{projectId}/languages',
    method: 'get',
    path: { projectId: projectDTO.id },
    query: {
      page: 0,
      size: 1000,
      sort: ['tag'],
    },
  });

  if (languagesLoadable.isFetching) {
    return <FullPageLoading />;
  }

  if (languagesLoadable.data) {
    return (
      <ProjectLanguagesContext.Provider value={languagesLoadable.data}>
        {props.children}
      </ProjectLanguagesContext.Provider>
    );
  }

  throw new GlobalError(
    'Unexpected error occurred',
    languagesLoadable.error?.code || 'Loadable error'
  );
};
