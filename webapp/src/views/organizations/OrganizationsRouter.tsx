import { Box } from '@mui/material';
import { Switch } from 'react-router-dom';

import { BoxLoading } from 'tg.component/common/BoxLoading';
import { PrivateRoute } from 'tg.component/common/PrivateRoute';
import { DashboardPage } from 'tg.component/layout/DashboardPage';
import { LINKS } from 'tg.constants/links';
import { useConfig, useIsAdmin } from 'tg.globalContext/helpers';

import { OrganizationCreateView } from './OrganizationCreateView';
import { OrganizationMemberPrivilegesView } from './OrganizationMemberPrivilegesView';
import { OrganizationMembersView } from './members/OrganizationMembersView';
import { OrganizationProfileView } from './OrganizationProfileView';
import { useOrganization } from './useOrganization';
import { OrganizationBillingView } from 'tg.ee/billing/OrganizationBillingView';
import { OrganizationInvoicesView } from 'tg.ee/billing/Invoices/OrganizationInvoicesView';
import { OrganizationSubscriptionsView } from 'tg.ee/billing/Subscriptions/OrganizationSubscriptionsView';
import { OrganizationBillingTestClockHelperView } from 'tg.ee/billing/OrganizationBillingTestClockHelperView';
import { OrganizationAppsView } from './apps/OrganizationAppsView';

const SpecificOrganizationRouter = () => {
  const organization = useOrganization();
  const config = useConfig();
  const isAdmin = useIsAdmin();
  const isAdminAccess =
    organization && organization?.currentUserRole !== 'OWNER' && isAdmin;

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
          {config.billing.enabled && (
            <>
              <PrivateRoute path={LINKS.ORGANIZATION_SUBSCRIPTIONS.template}>
                <OrganizationSubscriptionsView />
              </PrivateRoute>
              <PrivateRoute path={LINKS.ORGANIZATION_INVOICES.template}>
                <OrganizationInvoicesView />
              </PrivateRoute>
              <PrivateRoute path={LINKS.ORGANIZATION_BILLING.template}>
                <OrganizationBillingView />
              </PrivateRoute>
              <PrivateRoute path={LINKS.ORGANIZATION_BILLING.template}>
                <OrganizationBillingView />
              </PrivateRoute>
              {config.internalControllerEnabled && (
                <PrivateRoute
                  path={LINKS.ORGANIZATION_BILLING_TEST_CLOCK_HELPER.template}
                >
                  <OrganizationBillingTestClockHelperView />
                </PrivateRoute>
              )}
            </>
          )}
          <PrivateRoute path={LINKS.ORGANIZATION_APPS.template}>
            <OrganizationAppsView />
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
      <PrivateRoute exact path={LINKS.ORGANIZATIONS_ADD.template}>
        <OrganizationCreateView />
      </PrivateRoute>

      <PrivateRoute path={LINKS.ORGANIZATION.template}>
        <SpecificOrganizationRouter />
      </PrivateRoute>
    </Switch>
  );
};
