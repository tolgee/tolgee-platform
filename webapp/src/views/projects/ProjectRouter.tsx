import React from 'react';
import { Redirect, Route, Switch, useRouteMatch } from 'react-router-dom';

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
import { TrashView } from './translations/trash/TrashView';
import { SingleKeyView } from './translations/SingleKeyView';
import { DashboardView } from './dashboard/DashboardView';
import { WebsocketPreview } from './WebsocketPreview';
import { DeveloperView } from './developer/DeveloperView';
import { TaskRedirect } from './TaskRedirect';
import { routes } from 'tg.ee';
import { IntegrateView } from './integrate/IntegrateView';
import { AiView } from './ai/AiView';
import { BRANCH_ROUTES } from '../../branching/branchRoutes';

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

        <PrivateRoute exact path={LINKS.PROJECT_TRANSLATIONS_TRASH.template}>
          <TrashView />
        </PrivateRoute>

        <Route exact path={LINKS.PROJECT_TRANSLATIONS.template}>
          <TranslationsView />
        </Route>

        <PrivateRoute path={LINKS.PROJECT_EDIT.template}>
          <ProjectSettingsView />
        </PrivateRoute>

        <PrivateRoute path={LINKS.PROJECT_LANGUAGES.template}>
          <LanguageSettingsView />
        </PrivateRoute>

        <PrivateRoute exact path={LINKS.PROJECT_PERMISSIONS.template}>
          <ProjectMembersView />
        </PrivateRoute>

        <PrivateRoute exact path={LINKS.PROJECT_IMPORT.template}>
          <ImportView />
        </PrivateRoute>

        <Route path={LINKS.PROJECT_EXPORT.template}>
          <ExportView />
        </Route>

        <PrivateRoute exact path={LINKS.PROJECT_INTEGRATE.template}>
          <IntegrateView />
        </PrivateRoute>

        <Route exact path={LINKS.PROJECT_DASHBOARD.template}>
          <DashboardView />
        </Route>

        <PrivateRoute path={LINKS.PROJECT_DEVELOPER.template}>
          <DeveloperView />
        </PrivateRoute>

        {/* /ai redirects to the default Context Data tab; /ai/* renders AiView. */}
        <PrivateRoute exact path={LINKS.PROJECT_AI.template}>
          <Redirect
            to={LINKS.PROJECT_CONTEXT_DATA.build({
              [PARAMS.PROJECT_ID]: projectId,
            })}
          />
        </PrivateRoute>
        <PrivateRoute path={LINKS.PROJECT_AI.template}>
          <AiView />
        </PrivateRoute>

        <PrivateRoute path={LINKS.GO_TO_PROJECT_ACTIVITY_DETAIL.template}>
          <ActivityDetailRedirect />
        </PrivateRoute>

        <PrivateRoute path={LINKS.GO_TO_PROJECT_TASK.template}>
          <TaskRedirect />
        </PrivateRoute>

        {/* Internal preview route (websockets debugging). */}
        <PrivateRoute exact path={LINKS.PROJECT_WEBSOCKETS_PREVIEW.template}>
          <WebsocketPreview />
        </PrivateRoute>

        {/*Branched views*/}
        <Route exact path={BRANCH_ROUTES.translation.branched}>
          <SingleKeyView />
        </Route>

        <Route exact path={BRANCH_ROUTES.translations.branched}>
          <TranslationsView />
        </Route>

        <Route exact path={BRANCH_ROUTES.dashboard.branched}>
          <DashboardView />
        </Route>

        <PrivateRoute exact path={BRANCH_ROUTES.import.branched}>
          <ImportView />
        </PrivateRoute>

        <Route exact path={BRANCH_ROUTES.export.branched}>
          <ExportView />
        </Route>

        <PrivateRoute
          exact
          path={LINKS.PROJECT_TRANSLATIONS_TRASH_BRANCHED.template}
        >
          <TrashView />
        </PrivateRoute>
        {/* Guest catch-all: a route guests may open must sit above this line. Members match it too and render nothing. */}
        <PrivateRoute />
      </Switch>
      <routes.Project />
    </ProjectContext>
  );
};
