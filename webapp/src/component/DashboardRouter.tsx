import { Redirect, Route, Switch } from 'react-router-dom';

import { LINKS } from 'tg.constants/links';
import { ProjectsRouter } from 'tg.views/projects/ProjectsRouter';
import { CommunityProjectsView } from 'tg.views/projects/CommunityProjectsView';
import { OrganizationsRouter } from 'tg.views/organizations/OrganizationsRouter';
import { RootView } from 'tg.views/RootView';

import { PrivateRoute } from './common/PrivateRoute';
import { RequirePreferredOrganization } from './common/RequirePreferredOrganization';
import { HelpMenu } from './HelpMenu';

export const DashboardRouter = () => {
  return (
    <>
      <Switch>
        {/* Must stay above the gate: these are what a user with no preference
            can reach, and ProjectContext is what then adopts one. */}
        <PrivateRoute exact path={LINKS.PROJECTS.template}>
          <Redirect to={LINKS.ROOT.template} />
        </PrivateRoute>
        <PrivateRoute path={LINKS.PROJECTS.template}>
          <ProjectsRouter />
        </PrivateRoute>
        <PrivateRoute exact path={LINKS.COMMUNITY_PROJECTS.template}>
          <CommunityProjectsView />
        </PrivateRoute>

        <Route>
          <RequirePreferredOrganization>
            <Switch>
              <PrivateRoute exact path={LINKS.ROOT.template}>
                <RootView />
              </PrivateRoute>
              <PrivateRoute path={LINKS.ORGANIZATIONS.template}>
                <OrganizationsRouter />
              </PrivateRoute>
            </Switch>
          </RequirePreferredOrganization>
        </Route>
      </Switch>

      <HelpMenu />
    </>
  );
};
