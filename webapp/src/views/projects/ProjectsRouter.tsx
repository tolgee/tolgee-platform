import { Switch } from 'react-router-dom';

import { PrivateRoute } from 'tg.component/common/PrivateRoute';
import { LINKS } from 'tg.constants/links';
import { RequirePreferredOrganization } from 'tg.component/common/RequirePreferredOrganization';
import { ProjectRouter } from './ProjectRouter';
import { ProjectCreateView } from './project/ProjectCreateView';

export const ProjectsRouter = () => {
  return (
    <Switch>
      <PrivateRoute exact path={LINKS.PROJECT_ADD.template}>
        <RequirePreferredOrganization>
          <ProjectCreateView />
        </RequirePreferredOrganization>
      </PrivateRoute>

      <PrivateRoute path={LINKS.PROJECT.template}>
        <ProjectRouter />
      </PrivateRoute>
    </Switch>
  );
};
