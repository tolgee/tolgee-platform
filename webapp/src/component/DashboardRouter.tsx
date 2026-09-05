import { Redirect, Switch } from 'react-router-dom';

import { HelpMenu } from 'tg.component/HelpMenu';
import { PrivateRoute } from 'tg.component/common/PrivateRoute';
import { RequirePreferredOrganization } from 'tg.component/common/RequirePreferredOrganization';
import { LINKS } from 'tg.constants/links';
import { RootView } from 'tg.views/RootView';
import { OrganizationsRouter } from 'tg.views/organizations/OrganizationsRouter';
import { CommunityProjectsView } from 'tg.views/projects/CommunityProjectsView';
import { ProjectsRouter } from 'tg.views/projects/ProjectsRouter';

export const DashboardRouter = () => {
  return (
    <>
      <Switch>
        <PrivateRoute exact path={LINKS.PROJECTS.template}>
          <Redirect to={LINKS.ROOT.template} />
        </PrivateRoute>
        <PrivateRoute path={LINKS.PROJECTS.template}>
          <ProjectsRouter />
        </PrivateRoute>
        <PrivateRoute exact path={LINKS.COMMUNITY_PROJECTS.template}>
          <CommunityProjectsView />
        </PrivateRoute>
        <PrivateRoute exact path={LINKS.ROOT.template}>
          <RequirePreferredOrganization>
            <RootView />
          </RequirePreferredOrganization>
        </PrivateRoute>
        <PrivateRoute path={LINKS.ORGANIZATIONS.template}>
          <RequirePreferredOrganization>
            <OrganizationsRouter />
          </RequirePreferredOrganization>
        </PrivateRoute>
      </Switch>

      <HelpMenu />
    </>
  );
};
