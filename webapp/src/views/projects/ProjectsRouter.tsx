import { Switch } from 'react-router-dom';

import { PrivateRoute } from 'tg.component/common/PrivateRoute';
import { LINKS } from 'tg.constants/links';
import { ProjectRouter } from 'tg.views/projects/ProjectRouter';
import { ProjectCreateView } from 'tg.views/projects/project/ProjectCreateView';
import { RequirePreferredOrganization } from 'tg.component/common/RequirePreferredOrganization';

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
