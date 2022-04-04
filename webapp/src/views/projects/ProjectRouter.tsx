import { Route, Switch, useRouteMatch } from 'react-router-dom';

import { PrivateRoute } from 'tg.component/common/PrivateRoute';
import { LINKS, PARAMS } from 'tg.constants/links';
import { ProjectProvider } from 'tg.hooks/ProjectProvider';

import { ProjectPage } from './ProjectPage';
import { ExportView } from './export/ExportView';
import { ImportView } from './import/ImportView';
import { LanguageEditView } from './languages/LanguageEdit/LanguageEditView';
import { ProjectMembersView } from './members/ProjectMembersView';
import { ProjectSettingsView } from './project/ProjectSettingsView';
import { SocketIoPreview } from 'tg.views/projects/SocketIoPreview';
import { TranslationsView } from './translations/TranslationsView';
import { ProjectLanguagesView } from 'tg.views/projects/languages/ProjectLanguagesView';
import { SingleKeyView } from './translations/SingleKeyView';
import React from 'react';
import { FullPageLoading } from 'tg.component/common/FullPageLoading';
import { ActivityPreview } from './activity/ActivityPreview';

const IntegrateView = React.lazy(() =>
  import('tg.views/projects/integrate/IntegrateView').then((r) => ({
    default: r.IntegrateView,
  }))
);

export const ProjectRouter = () => {
  const match = useRouteMatch();

  const projectId = match.params[PARAMS.PROJECT_ID];

  const matchedTranslations = useRouteMatch(
    LINKS.PROJECT_TRANSLATIONS.template
  );

  return (
    <Switch>
      <ProjectProvider id={Number(projectId)}>
        <ProjectPage topBarAutoHide={matchedTranslations?.isExact}>
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

            <Route exact path={LINKS.PROJECT_PERMISSIONS.template}>
              <ProjectMembersView />
            </Route>

            <PrivateRoute exact path={LINKS.PROJECT_IMPORT.template}>
              <ImportView />
            </PrivateRoute>

            <Route exact path={LINKS.PROJECT_EXPORT.template}>
              <ExportView />
            </Route>

            <Route exact path={LINKS.PROJECT_INTEGRATE.template}>
              <IntegrateView />
            </Route>

            {/*
              Preview section...
            */}

            <Route exact path={LINKS.PROJECT_SOCKET_IO_PREVIEW.template}>
              <SocketIoPreview />
            </Route>

            <Route exact path={LINKS.ACTIVITY_PREVIEW.template}>
              <ActivityPreview />
            </Route>
          </React.Suspense>
        </ProjectPage>
      </ProjectProvider>
    </Switch>
  );
};
