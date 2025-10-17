import { Box } from '@mui/material';
import { Switch } from 'react-router-dom';

import { BoxLoading } from 'tg.component/common/BoxLoading';
import { PrivateRoute } from 'tg.component/common/PrivateRoute';
import { DashboardPage } from 'tg.component/layout/DashboardPage';
import { LINKS } from 'tg.constants/links';
import { useIsAdminOrSupporter } from 'tg.globalContext/helpers';

import { OrganizationCreateView } from './OrganizationCreateView';
import { OrganizationMemberPrivilegesView } from './OrganizationMemberPrivilegesView';
import { OrganizationMembersView } from './members/OrganizationMembersView';
import { OrganizationProfileView } from './OrganizationProfileView';
import { useOrganization } from './useOrganization';
import { OrganizationAppsView } from './apps/OrganizationAppsView';
import { routes } from 'tg.ee';

const SpecificOrganizationRouter = () => {
  const organization = useOrganization();
  const isAdminOrSupporter = useIsAdminOrSupporter();
  const isAdminAccess =
    organization &&
    organization?.currentUserRole !== 'OWNER' &&
    isAdminOrSupporter;

  return (
    <DashboardPage isAdminAccess={isAdminAccess}>
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

          <PrivateRoute path={LINKS.ORGANIZATION_APPS.template}>
            <OrganizationAppsView />
          </PrivateRoute>
          <routes.Organization />
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
    <>
      <Switch>
        <PrivateRoute exact path={LINKS.ORGANIZATIONS_ADD.template}>
          <OrganizationCreateView />
        </PrivateRoute>

        <PrivateRoute path={LINKS.ORGANIZATION.template}>
          <SpecificOrganizationRouter />
        </PrivateRoute>
      </Switch>
    </>
  );
};
