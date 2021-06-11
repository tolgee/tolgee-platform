import { Route, Switch, useRouteMatch } from 'react-router-dom';
import { LanguageListView } from './languages/LanguageListView';
import { LINKS, PARAMS } from '../../../constants/links';
import { LanguageEditView } from './languages/LanguageEditView';
import { ProjectSettingsView } from './project/ProjectSettingsView';
import { ProjectProvider } from '../../../hooks/ProjectProvider';
import { LanguageCreateView } from './languages/LanguageCreateView';
import { PrivateRoute } from '../../common/PrivateRoute';
import { ImportView } from './imprt_export/ImportView';
import { ExportView } from './imprt_export/ExportView';
import { ProjectPage } from './ProjectPage';
import { TranslationView } from './translations/TranslationView';
import { ProjectPermissionsView } from './permissions/ProjectPermissionsVIew';
import { ProjectInviteView } from './invitations/ProjectInviteView';

export const ProjectRouter = () => {
  const match = useRouteMatch();

  const projectId = match.params[PARAMS.PROJECT_ID];

  return (
    <Switch>
      <ProjectProvider id={Number(projectId)}>
        <ProjectPage fullWidth={true}>
          <Route path={LINKS.PROJECT_TRANSLATIONS.template}>
            <TranslationView />
          </Route>

          <Route exact path={LINKS.PROJECT_EDIT.template}>
            <ProjectSettingsView />
          </Route>

          <Route exact path={LINKS.PROJECT_LANGUAGES.template}>
            <LanguageListView />
          </Route>

          <Route exact path={LINKS.PROJECT_LANGUAGES_CREATE.template}>
            <LanguageCreateView />
          </Route>

          <Route exact path={LINKS.PROJECT_LANGUAGE_EDIT.template}>
            <LanguageEditView />
          </Route>

          <Route exact path={LINKS.PROJECT_INVITATION.template}>
            <ProjectInviteView />
          </Route>

          <Route exact path={LINKS.PROJECT_PERMISSIONS.template}>
            <ProjectPermissionsView />
          </Route>

          <PrivateRoute exact path={LINKS.PROJECT_IMPORT.template}>
            <ImportView />
          </PrivateRoute>

          <Route exact path={LINKS.PROJECT_EXPORT.template}>
            <ExportView />
          </Route>
        </ProjectPage>
      </ProjectProvider>
    </Switch>
  );
};
