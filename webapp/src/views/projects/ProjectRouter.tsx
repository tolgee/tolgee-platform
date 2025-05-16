import React from 'react';
import { Route, Switch, useRouteMatch } from 'react-router-dom';

import { PrivateRoute } from 'tg.component/common/PrivateRoute';
import { LINKS, PARAMS } from 'tg.constants/links';
import { ProjectContext } from 'tg.hooks/ProjectContext';
import { HideObserver } from 'tg.component/layout/TopBar/HideObserver';
import { ActivityDetailRedirect } from 'tg.component/security/ActivityDetailRedirect';
import { LanguageSettingsView } from 'tg.views/projects/languages/LanguageSettingsView';

import { ExportView } from './export/ExportView';
import { ImportView } from './import/ImportView';
import { ProjectMembersView } from './members/ProjectMembersView';
import { ProjectSettingsView } from './project/ProjectSettingsView';
import { TranslationsView } from './translations/TranslationsView';
import { SingleKeyView } from './translations/SingleKeyView';
import { DashboardView } from './dashboard/DashboardView';
import { WebsocketPreview } from './WebsocketPreview';
import { DeveloperView } from './developer/DeveloperView';
import { TaskRedirect } from './TaskRedirect';
import { routes } from 'tg.ee';
import { IntegrateView } from './integrate/IntegrateView';
import { AiView } from './ai/AiView';

export const ProjectRouter = () => {
  const match = useRouteMatch();

  const projectId = match.params[PARAMS.PROJECT_ID];

  const matchedTranslations = useRouteMatch(
    LINKS.PROJECT_TRANSLATIONS.template
  );

  return (
    <ProjectContext id={Number(projectId)}>
      {matchedTranslations?.isExact && <HideObserver />}
      <Switch>
        <Route exact path={LINKS.PROJECT_TRANSLATIONS_SINGLE.template}>
          <SingleKeyView />
        </Route>

        <Route exact path={LINKS.PROJECT_TRANSLATIONS.template}>
          <TranslationsView />
        </Route>

        <Route path={LINKS.PROJECT_EDIT.template}>
          <ProjectSettingsView />
        </Route>

        <Route path={LINKS.PROJECT_LANGUAGES.template}>
          <LanguageSettingsView />
        </Route>

        <Route exact path={LINKS.PROJECT_PERMISSIONS.template}>
          <ProjectMembersView />
        </Route>

        <PrivateRoute exact path={LINKS.PROJECT_IMPORT.template}>
          <ImportView />
        </PrivateRoute>

        <Route path={LINKS.PROJECT_EXPORT.template}>
          <ExportView />
        </Route>

        <Route exact path={LINKS.PROJECT_INTEGRATE.template}>
          <IntegrateView />
        </Route>

        <Route exact path={LINKS.PROJECT_DASHBOARD.template}>
          <DashboardView />
        </Route>

        <Route path={LINKS.PROJECT_DEVELOPER.template}>
          <DeveloperView />
        </Route>

        <Route path={LINKS.PROJECT_AI.template}>
          <AiView />
        </Route>

        <Route path={LINKS.GO_TO_PROJECT_ACTIVITY_DETAIL.template}>
          <ActivityDetailRedirect />
        </Route>

        <Route path={LINKS.GO_TO_PROJECT_TASK.template}>
          <TaskRedirect />
        </Route>

        {/*
          Preview section...
        */}
        <Route exact path={LINKS.PROJECT_WEBSOCKETS_PREVIEW.template}>
          <WebsocketPreview />
        </Route>
      </Switch>
      <routes.Project />
    </ProjectContext>
  );
};
