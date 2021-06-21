import { FunctionComponent, useEffect } from 'react';
import { container } from 'tsyringe';
import { useSelector } from 'react-redux';
import { AppState } from '../store';
import { GlobalError } from '../error/GlobalError';
import { LanguageActions } from '../store/languages/LanguageActions';
import { useProject } from './useProject';
import { FullPageLoading } from '../component/common/FullPageLoading';
import { ProjectPreferencesService } from '../service/ProjectPreferencesService';
import { TranslationActions } from '../store/project/TranslationActions';

const languageActions = container.resolve(LanguageActions);
const translationActions = container.resolve(TranslationActions);
const selectedLanguagesService = container.resolve(ProjectPreferencesService);

export const ProjectLanguagesProvider: FunctionComponent = (props) => {
  const projectDTO = useProject();

  const languagesLoadable = useSelector(
    (state: AppState) => state.languages.loadables.globalList
  );
  const selectedLanguages = useSelector(
    (state: AppState) => state.translations.selectedLanguages
  );

  const isLoading = languagesLoadable.loading;
  const init =
    !languagesLoadable.data && !languagesLoadable.error && !isLoading;
  const idChanged =
    languagesLoadable.dispatchParams &&
    languagesLoadable.dispatchParams[0].path.projectId !== projectDTO.id;

  useEffect(() => {
    if (init || idChanged) {
      languageActions.loadableActions.globalList.dispatch({
        path: { projectId: projectDTO.id },
        query: {
          pageable: {
            page: 0,
            size: 1000,
            sort: ['tag'],
          },
        },
      });
    }
  }, [init]);

  useEffect(() => {
    // reset languages when unmount
    return () => {
      languageActions.loadableReset.globalList.dispatch();
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

  if (init || idChanged || languagesLoadable.loading || !selectedLanguages) {
    return <FullPageLoading />;
  }

  if (languagesLoadable.data) {
    return <>{props.children}</>;
  }

  throw new GlobalError(
    'Unexpected error occurred',
    languagesLoadable.error?.code || 'Loadable error'
  );
};
