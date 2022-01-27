import { Route, Switch, useRouteMatch } from 'react-router-dom';

import { PrivateRoute } from 'tg.component/common/PrivateRoute';
import { LINKS, PARAMS } from 'tg.constants/links';
import { ProjectProvider } from 'tg.hooks/ProjectProvider';

import { ProjectPage } from './ProjectPage';
import { ExportView } from './export/ExportView';
import { ImportView } from './import/ImportView';
import { ProjectInviteView } from './invitations/ProjectInviteView';
import { LanguageEditView } from './languages/LanguageEdit/LanguageEditView';
import { ProjectPermissionsView } from './permissions/ProjectPermissionsVIew';
import { ProjectSettingsView } from './project/ProjectSettingsView';
import { SocketIoPreview } from 'tg.views/projects/SocketIoPreview';
import { TranslationsView } from './translations/TranslationsView';
import { ProjectLanguagesView } from 'tg.views/projects/languages/ProjectLanguagesView';
import { SingleKeyView } from './translations/SingleKeyView';
import React from 'react';
import { FullPageLoading } from 'tg.component/common/FullPageLoading';

const IntegrateView = React.lazy(() =>
  import('tg.views/projects/integrate/IntegrateView').then((r) => ({
    default: r.IntegrateView,
  }))
);

export const ProjectRouter = () => {
  const match = useRouteMatch();

  const projectId = match.params[PARAMS.PROJECT_ID];

  return (
    <Switch>
      <ProjectProvider id={Number(projectId)}>
        <ProjectPage fullWidth={true}>
          <React.Suspense fallback={<FullPageLoading />}>
            <Route exact path={LINKS.PROJECT_TRANSLATIONS_SINGLE.template}>
              <SingleKeyView />
            </Route>

            <Route exact path={LINKS.PROJECT_TRANSLATIONS.template}>
              <TranslationsView />
            </Route>

            <Route exact path={LINKS.PROJECT_EDIT.template}>
              <ProjectSettingsView />
            </Route>

            <Route exact path={LINKS.PROJECT_LANGUAGES.template}>
              <ProjectLanguagesView />
            </Route>

            <Route exact path={LINKS.PROJECT_EDIT_LANGUAGE.template}>
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

            <Route exact path={LINKS.PROJECT_INTEGRATE.template}>
              <IntegrateView />
            </Route>
          </React.Suspense>
        </ProjectPage>
      </ProjectProvider>
    </Switch>
  );
};
