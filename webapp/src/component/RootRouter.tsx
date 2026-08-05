import React from 'react';
import { Route, Switch } from 'react-router-dom';

import { LINKS } from 'tg.constants/links';
import { UserSettingsRouter } from 'tg.views/userSettings/UserSettingsRouter';
import { AdministrationView } from 'tg.views/administration/AdministrationView';
import { routes } from 'tg.ee';

import { PrivateRoute } from './common/PrivateRoute';
import { DashboardRouter } from './DashboardRouter';
import { OrganizationBillingRedirect } from './security/OrganizationBillingRedirect';
import { PublicOnlyRoute } from './common/PublicOnlyRoute';
import { PreferredOrganizationRedirect } from './security/PreferredOrganizationRedirect';
import { RecaptchaProvider } from 'tg.component/common/RecaptchaProvider';

const LoginRouter = React.lazy(() => import('./security/Login/LoginRouter'));

const SlackConnectView = React.lazy(() => import('./slack/SlackConnectView'));

const SlackConnectedView = React.lazy(
  () => import('./slack/SlackConnectedView')
);

const SignUpView = React.lazy(() => import('./security/SignUp/SignUpView'));

const PublicProjectListView = React.lazy(
  () => import('tg.views/projects/public/PublicProjectListView')
);

const PasswordResetSetView = React.lazy(
  () => import('./security/ResetPasswordSetView')
);
const PasswordResetView = React.lazy(
  () => import('./security/ResetPasswordView')
);
const AcceptInvitationView = React.lazy(
  () => import('./security/AcceptInvitationView')
);
const SsoMigrationView = React.lazy(
  () => import('./security/SsoMigrationView')
);
const AcceptAuthProviderChangeView = React.lazy(
  () => import('./security/AcceptAuthProviderChangeView')
);

export const RootRouter = () => {
  return (
    <>
      <Switch>
        <PrivateRoute exact path={LINKS.SLACK_CONNECT.template}>
          <SlackConnectView />
        </PrivateRoute>
        <PrivateRoute exact path={LINKS.SLACK_CONNECTED.template}>
          <SlackConnectedView />
        </PrivateRoute>
        <Route exact path={LINKS.RESET_PASSWORD_REQUEST.template}>
          <PasswordResetView />
        </Route>
        <Route exact path={LINKS.RESET_PASSWORD_WITH_PARAMS.template}>
          <PasswordResetSetView />
        </Route>
        <PublicOnlyRoute exact path={LINKS.SIGN_UP.template}>
          <RecaptchaProvider>
            <SignUpView />
          </RecaptchaProvider>
        </PublicOnlyRoute>
        <Route path={LINKS.LOGIN.template}>
          <LoginRouter />
        </Route>
        <Route exact path={LINKS.PUBLIC_PROJECTS.template}>
          <PublicProjectListView />
        </Route>
        <Route path={LINKS.ACCEPT_INVITATION.template}>
          <AcceptInvitationView />
        </Route>
        <PrivateRoute path={LINKS.SSO_MIGRATION.template}>
          <SsoMigrationView />
        </PrivateRoute>
        <PrivateRoute path={LINKS.ACCEPT_AUTH_PROVIDER_CHANGE.template}>
          <AcceptAuthProviderChangeView />
        </PrivateRoute>
        <PrivateRoute path={LINKS.GO_TO_CLOUD_BILLING.template}>
          <OrganizationBillingRedirect selfHosted={false} />
        </PrivateRoute>
        <PrivateRoute path={LINKS.GO_TO_SELF_HOSTED_BILLING.template}>
          <OrganizationBillingRedirect selfHosted={true} />
        </PrivateRoute>
        <PrivateRoute path={LINKS.GO_TO_PREFERRED_ORGANIZATION.template}>
          <PreferredOrganizationRedirect />
        </PrivateRoute>
        <PrivateRoute path={LINKS.USER_SETTINGS.template}>
          <UserSettingsRouter />
        </PrivateRoute>
        <PrivateRoute path={LINKS.ADMINISTRATION.template}>
          <AdministrationView />
        </PrivateRoute>

        <Route>
          <DashboardRouter />
        </Route>
      </Switch>

      <routes.Root />
    </>
  );
};
