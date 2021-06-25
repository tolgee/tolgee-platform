import { Switch, useRouteMatch } from 'react-router-dom';

import { PrivateRoute } from 'tg.component/common/PrivateRoute';
import { LINKS } from 'tg.constants/links';

import { ProjectListView } from './ProjectListView';
import { ProjectRouter } from './ProjectRouter';
import { ProjectCreateView } from './project/ProjectCreateView';

export const ProjectsRouter = () => {
  const match = useRouteMatch();

  return (
    <Switch>
      <PrivateRoute exact path={`${match.path}`}>
        <ProjectListView />
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
