import { Route, Switch, useRouteMatch } from 'react-router-dom';
import { LanguageListView } from './languages/LanguageListView';
import { LINKS, PARAMS } from '../../../constants/links';
import { LanguageEditView } from './languages/LanguageEditView';
import { ProjectInviteView } from './invitations/ProjectInviteView';
import { ProjectPermissionsView } from './permissions/ProjectPermissionsVIew';
import { ProjectSettingsView } from './project/ProjectSettingsView';
import { ProjectProvider } from '../../../hooks/ProjectProvider';
import { LanguageCreateView } from './languages/LanguageCreateView';
import { PrivateRoute } from '../../common/PrivateRoute';
import { ImportView } from './imprt_export/ImportView';
import { ExportView } from './imprt_export/ExportView';
import { RepositoryPage } from './RepositoryPage';
import { TranslationView } from './translations/TranslationView';
import Overview from './overview/Overview';

export const ProjectRouter = () => {
  let match = useRouteMatch();

  const repositoryId = match.params[PARAMS.REPOSITORY_ID];

  return (
    <Switch>
      <RepositoryProvider id={repositoryId}>
        <RepositoryPage fullWidth={true}>
          <Route exact path={LINKS.REPOSITORY.template}>
            <Overview />
          </Route>

          <Route path={LINKS.REPOSITORY_TRANSLATIONS.template}>
            <TranslationView />
          </Route>

          <Route exact path={LINKS.REPOSITORY_EDIT.template}>
            <ProjectSettingsView />
          </Route>

          <Route exact path={`${LINKS.REPOSITORY_LANGUAGES.template}`}>
            <LanguageListView />
          </Route>

          <Route exact path={`${LINKS.REPOSITORY_LANGUAGES_CREATE.template}`}>
            <LanguageCreateView />
          </Route>

          <Route exact path={`${LINKS.REPOSITORY_LANGUAGE_EDIT.template}`}>
            <LanguageEditView />
          </Route>

          <Route exact path={`${LINKS.REPOSITORY_INVITATION.template}`}>
            <ProjectInviteView />
          </Route>

          <Route exact path={`${LINKS.REPOSITORY_PERMISSIONS.template}`}>
            <ProjectPermissionsView />
          </Route>

          <PrivateRoute exact path={LINKS.REPOSITORY_IMPORT.template}>
            <ImportView />
          </PrivateRoute>

          <Route exact path={`${LINKS.REPOSITORY_EXPORT.template}`}>
            <ExportView />
          </Route>
        </ProjectPage>
      </ProjectProvider>
    </Switch>
  );
};
