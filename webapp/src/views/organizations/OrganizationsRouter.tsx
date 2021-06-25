import { Switch } from 'react-router-dom';
import { PrivateRoute } from 'tg.component/common/PrivateRoute';
import { OrganizationsListView } from './OrganizationListView';
import { OrganizationCreateView } from './OrganizationCreateView';
import { LINKS } from 'tg.constants/links';
import { OrganizationProfileView } from './OrganizationProfileView';
import { BoxLoading } from 'tg.component/common/BoxLoading';
import { DashboardPage } from 'tg.component/layout/DashboardPage';
import { OrganizationMembersView } from './OrganizationMembersView';
import { OrganizationMemberPrivilegesView } from './OrganizationMemberPrivilegesView';
import { OrganizationInvitationsView } from './OrganizationInvitationsView';
import { OrganizationsProjectListView } from './OrganizationProjectListView';

import { useOrganization } from './useOrganization';

const SpecificOrganizationRouter = () => {
  const organization = useOrganization();

  return (
    <DashboardPage>
      {organization ? (
        <>
          <PrivateRoute exact path={LINKS.ORGANIZATION_PROFILE.template}>
            <OrganizationProfileView />
          </PrivateRoute>
          <PrivateRoute exact path={LINKS.ORGANIZATION_MEMBERS.template}>
            <OrganizationMembersView />
          </PrivateRoute>
          <PrivateRoute
            exact
            path={LINKS.ORGANIZATION_MEMBER_PRIVILEGES.template}
          >
            <OrganizationMemberPrivilegesView />
          </PrivateRoute>
          <PrivateRoute exact path={LINKS.ORGANIZATION_INVITATIONS.template}>
            <OrganizationInvitationsView />
          </PrivateRoute>
          <PrivateRoute exact path={LINKS.ORGANIZATION_PROJECTS.template}>
            <OrganizationsProjectListView />
          </PrivateRoute>
        </>
      ) : (
        <BoxLoading />
      )}
    </DashboardPage>
  );
};

export const OrganizationsRouter = () => {
  return (
    <Switch>
      <PrivateRoute exact path={LINKS.ORGANIZATIONS.template}>
        <OrganizationsListView />
      </PrivateRoute>

      <PrivateRoute exact path={LINKS.ORGANIZATIONS_ADD.template}>
        <OrganizationCreateView />
      </PrivateRoute>

      <PrivateRoute path={LINKS.ORGANIZATION.template}>
        <SpecificOrganizationRouter />
      </PrivateRoute>
    </Switch>
  );
};
