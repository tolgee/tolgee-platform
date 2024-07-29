import { Switch, useRouteMatch } from 'react-router-dom';

import { PrivateRoute } from 'tg.component/common/PrivateRoute';
import { LINKS } from 'tg.constants/links';
import { ProjectRouter } from './ProjectRouter';
import { ProjectCreateView } from './project/ProjectCreateView';
import React from 'react';
import { RootView } from 'tg.views/RootView';

export const ProjectsRouter = () => {
  const match = useRouteMatch();

  return (
    <Switch>
      <PrivateRoute exact path={`${match.path}`}>
        <RootView />
      </PrivateRoute>

      <PrivateRoute exact path={`${LINKS.PROJECT_ADD.template}`}>
        <ProjectCreateView />
      </PrivateRoute>

      <PrivateRoute path={LINKS.PROJECT.template}>
        <ProjectRouter />
      </PrivateRoute>
    </Switch>
  );
};
