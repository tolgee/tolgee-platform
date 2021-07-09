import { Route, Switch, useRouteMatch } from 'react-router-dom';

import { PrivateRoute } from 'tg.component/common/PrivateRoute';
import { LINKS, PARAMS } from 'tg.constants/links';
import { ProjectProvider } from 'tg.hooks/ProjectProvider';

import { ProjectPage } from './ProjectPage';
import { ExportView } from './imprt_export/ExportView';
import { ImportView } from './imprt_export/ImportView';
import { ProjectInviteView } from './invitations/ProjectInviteView';
import { LanguageEditView } from './languages/LanguageEditView';
import { ProjectPermissionsView } from './permissions/ProjectPermissionsVIew';
import { ProjectSettingsView } from './project/ProjectSettingsView';
import { TranslationView } from './translations/TranslationView';
import { SocketIoPreview } from 'tg.views/projects/SocketIoPreview';
import { TranslationsView } from './translations/TranslationsView';

export const ProjectRouter = () => {
  const match = useRouteMatch();

  const projectId = match.params[PARAMS.PROJECT_ID];

  return (
    <Switch>
      <ProjectProvider id={Number(projectId)}>
        <ProjectPage fullWidth={true}>
          <Route path={LINKS.PROJECT_TRANSLATIONS.template}>
            <TranslationsView />
          </Route>

          <Route exact path={LINKS.PROJECT_EDIT.template}>
            <ProjectSettingsView />
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

          <Route exact path={LINKS.PROJECT_SOCKET_IO_PREVIEW.template}>
            <SocketIoPreview />
          </Route>
        </ProjectPage>
      </ProjectProvider>
    </Switch>
  );
};
