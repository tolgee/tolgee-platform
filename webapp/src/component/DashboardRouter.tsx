import { Redirect, Switch } from 'react-router-dom';

import { LINKS } from 'tg.constants/links';
import { ProjectsRouter } from 'tg.views/projects/ProjectsRouter';
import { CommunityProjectsView } from 'tg.views/projects/CommunityProjectsView';
import { OrganizationsRouter } from 'tg.views/organizations/OrganizationsRouter';
import { RootView } from 'tg.views/RootView';

import { PrivateRoute } from 'tg.component/common/PrivateRoute';
import { RequirePreferredOrganization } from 'tg.component/common/RequirePreferredOrganization';
import { HelpMenu } from 'tg.component/HelpMenu';
import { useIsEmailVerified } from 'tg.globalContext/helpers';

export const DashboardRouter = () => {
  const isEmailVerified = useIsEmailVerified();

  return (
    <>
      <Switch>
        <PrivateRoute exact path={LINKS.PROJECTS.template}>
          <Redirect to={LINKS.ROOT.build()} />
        </PrivateRoute>
        <PrivateRoute path={LINKS.PROJECTS.template}>
          <ProjectsRouter />
        </PrivateRoute>
        <PrivateRoute exact path={LINKS.COMMUNITY_PROJECTS.template}>
          <CommunityProjectsView />
        </PrivateRoute>
        <PrivateRoute exact path={LINKS.ROOT.template}>
          {isEmailVerified ? (
            <RequirePreferredOrganization
              fallback={<Redirect to={LINKS.COMMUNITY_PROJECTS.build()} />}
            >
              <RootView />
            </RequirePreferredOrganization>
          ) : (
            <RootView />
          )}
        </PrivateRoute>
        <PrivateRoute path={LINKS.ORGANIZATIONS.template}>
          <OrganizationsRouter />
        </PrivateRoute>
      </Switch>

      <HelpMenu />
    </>
  );
};
