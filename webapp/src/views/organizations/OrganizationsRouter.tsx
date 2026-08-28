import { Box } from '@mui/material';
import { Redirect, Switch } from 'react-router-dom';

import { BoxLoading } from 'tg.component/common/BoxLoading';
import { OrganizationLoadFailedView } from 'tg.component/common/OrganizationLoadFailedView';
import { PrivateRoute } from 'tg.component/common/PrivateRoute';
import { DashboardPage } from 'tg.component/layout/DashboardPage';
import { LINKS, PARAMS } from 'tg.constants/links';
import { useIsAdminOrSupporter } from 'tg.globalContext/helpers';
import { useOrganizationAdoption } from 'tg.globalContext/useOrganizationAdoption';
import { useOrganizationLoadable } from 'tg.views/organizations/useOrganization';

import { OrganizationCreateView } from './OrganizationCreateView';
import { OrganizationMemberPrivilegesView } from './OrganizationMemberPrivilegesView';
import { OrganizationMembersView } from './members/OrganizationMembersView';
import { OrganizationProfileView } from './OrganizationProfileView';
import { OrganizationAppsView } from './apps/OrganizationAppsView';
import { RequireOrganizationAccess } from 'tg.component/common/RequireOrganizationAccess';
import { isOwnerOrgRole } from 'tg.fixtures/organizationRole';
import { routes } from 'tg.ee';

const RedirectToOrganizationProfile = ({ slug }: { slug: string }) => (
  <Redirect
    to={LINKS.ORGANIZATION_PROFILE.build({ [PARAMS.ORGANIZATION_SLUG]: slug })}
  />
);

const SpecificOrganizationRouter = () => {
  const organization = useOrganizationLoadable();
  const isAdminOrSupporter = useIsAdminOrSupporter();
  const { awaitingOrganization } = useOrganizationAdoption(
    organization.data?.id
  );

  if (organization.isLoading || awaitingOrganization) {
    return (
      <DashboardPage>
        <Box
          width="100%"
          height="100%"
          display="flex"
          alignItems="center"
          justifyContent="center"
        >
          <BoxLoading />
        </Box>
      </DashboardPage>
    );
  }

  if (!organization.data) {
    return <OrganizationLoadFailedView />;
  }

  const isAdminAccess =
    !isOwnerOrgRole(organization.data.currentUserRole) && isAdminOrSupporter;

  return (
    <DashboardPage isAdminAccess={isAdminAccess}>
      <PrivateRoute exact path={LINKS.ORGANIZATION_PROFILE.template}>
        <OrganizationProfileView />
      </PrivateRoute>

      <PrivateRoute exact path={LINKS.ORGANIZATION_MEMBERS.template}>
        <RequireOrganizationAccess level="member">
          <OrganizationMembersView />
        </RequireOrganizationAccess>
      </PrivateRoute>

      <PrivateRoute exact path={LINKS.ORGANIZATION_MEMBER_PRIVILEGES.template}>
        <RequireOrganizationAccess level="manage">
          <OrganizationMemberPrivilegesView />
        </RequireOrganizationAccess>
      </PrivateRoute>

      <PrivateRoute path={LINKS.ORGANIZATION_APPS.template}>
        <RequireOrganizationAccess level="manage">
          <OrganizationAppsView />
        </RequireOrganizationAccess>
      </PrivateRoute>

      <PrivateRoute exact path={LINKS.ORGANIZATION.template}>
        <RedirectToOrganizationProfile slug={organization.data.slug} />
      </PrivateRoute>
      <routes.Organization />
    </DashboardPage>
  );
};

export const OrganizationsRouter = () => {
  return (
    <Switch>
      <PrivateRoute exact path={LINKS.ORGANIZATIONS_ADD.template}>
        <OrganizationCreateView />
      </PrivateRoute>

      <PrivateRoute path={LINKS.ORGANIZATION.template}>
        <SpecificOrganizationRouter />
      </PrivateRoute>
    </Switch>
  );
};
