import { Switch } from 'react-router-dom';

import { PrivateRoute } from 'tg.component/common/PrivateRoute';
import { LINKS } from 'tg.constants/links';
import { ProjectRouter } from './ProjectRouter';
import { ProjectCreateView } from './project/ProjectCreateView';

export const ProjectsRouter = () => {
  return (
    <Switch>
      <PrivateRoute exact path={LINKS.PROJECT_ADD.template}>
        <ProjectCreateView />
      </PrivateRoute>

      <PrivateRoute path={LINKS.PROJECT.template}>
        <ProjectRouter />
      </PrivateRoute>
    </Switch>
  );
};
