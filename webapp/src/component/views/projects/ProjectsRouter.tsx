import { Switch, useRouteMatch } from 'react-router-dom';
import * as React from 'react';
import { ProjectListView } from './ProjectListView';
import { ProjectCreateView } from './project/ProjectCreateView';
import { LINKS } from '../../../constants/links';
import { PrivateRoute } from '../../common/PrivateRoute';
import { ProjectRouter } from './ProjectRouter';

export const ProjectsRouter = () => {
  let match = useRouteMatch();

  return (
    <Switch>
      <PrivateRoute exact path={`${match.path}`}>
        <ProjectListView />
      </PrivateRoute>

      <PrivateRoute exact path={`${LINKS.REPOSITORY_ADD.template}`}>
        <ProjectCreateView />
      </PrivateRoute>

      <PrivateRoute path={LINKS.REPOSITORY.template}>
        <ProjectRouter />
      </PrivateRoute>
    </Switch>
  );
};
