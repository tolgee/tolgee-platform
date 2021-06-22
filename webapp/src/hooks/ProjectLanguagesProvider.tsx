import { FunctionComponent, useEffect, createContext } from 'react';
import { container } from 'tsyringe';
import { useSelector } from 'react-redux';
import { AppState } from '../store';
import { GlobalError } from '../error/GlobalError';
import { useProject } from './useProject';
import { FullPageLoading } from '../component/common/FullPageLoading';
import { ProjectPreferencesService } from '../service/ProjectPreferencesService';
import { TranslationActions } from '../store/project/TranslationActions';
import { useApiQuery } from '../service/http/useQueryApi';
import { components } from '../service/apiSchema.generated';

export const ProjectLanguagesContext =
  // @ts-ignore
  createContext<components['schemas']['PagedModelLanguageModel']>(null);

const translationActions = container.resolve(TranslationActions);
const selectedLanguagesService = container.resolve(ProjectPreferencesService);

export const ProjectLanguagesProvider: FunctionComponent = (props) => {
  const projectDTO = useProject();

  const selectedLanguages = useSelector(
    (state: AppState) => state.translations.selectedLanguages
  );

  const languagesLoadable = useApiQuery({
    url: '/v2/projects/{projectId}/languages',
    method: 'get',
    path: { projectId: projectDTO.id },
    query: {
      pageable: {
        page: 0,
        size: 1000,
        sort: ['tag'],
      },
    },
  });

  useEffect(() => {
    // reset languages when unmount
    return () => {
      languagesLoadable.remove();
    };
  }, []);

  useEffect(() => {
    translationActions.select.dispatch(null);
    if (languagesLoadable.data) {
      if (languagesLoadable.data._embedded?.languages?.length) {
        const selection = selectedLanguagesService.getUpdated(
          projectDTO.id,
          languagesLoadable.data._embedded.languages.map((l) => l.tag) || []
        );
        translationActions.select.dispatch(selection);
      } else {
        translationActions.select.dispatch(
          selectedLanguagesService.getForProject(projectDTO.id)
        );
      }
    }
  }, [languagesLoadable.data]);

  if (languagesLoadable.isFetching || !selectedLanguages) {
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
