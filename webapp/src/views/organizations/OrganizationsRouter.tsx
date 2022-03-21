import { Box } from '@material-ui/core';
import { Switch } from 'react-router-dom';

import { BoxLoading } from 'tg.component/common/BoxLoading';
import { PrivateRoute } from 'tg.component/common/PrivateRoute';
import { DashboardPage } from 'tg.component/layout/DashboardPage';
import { LINKS } from 'tg.constants/links';

import { OrganizationCreateView } from './OrganizationCreateView';
import { OrganizationsListView } from './OrganizationListView';
import { OrganizationMemberPrivilegesView } from './OrganizationMemberPrivilegesView';
import { OrganizationMembersView } from './members/OrganizationMembersView';
import { OrganizationProfileView } from './OrganizationProfileView';
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
          <PrivateRoute exact path={LINKS.ORGANIZATION_PROJECTS.template}>
            <OrganizationsProjectListView />
          </PrivateRoute>
        </>
      ) : (
        <Box
          width="100%"
          height="100%"
          display="flex"
          alignItems="center"
          justifyContent="center"
        >
          <BoxLoading />
        </Box>
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
