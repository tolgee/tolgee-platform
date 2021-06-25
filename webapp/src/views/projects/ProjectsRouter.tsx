import { Switch, useRouteMatch } from 'react-router-dom';
import { ProjectListView } from './ProjectListView';
import { ProjectCreateView } from './project/ProjectCreateView';
import { LINKS } from '../../constants/links';
import { PrivateRoute } from '../../component/common/PrivateRoute';
import { ProjectRouter } from './ProjectRouter';

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
