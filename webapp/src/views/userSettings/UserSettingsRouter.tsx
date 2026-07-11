import { Switch } from 'react-router-dom';

import { PrivateRoute } from 'tg.component/common/PrivateRoute';
import { LINKS } from 'tg.constants/links';

import { DashboardPage } from 'tg.component/layout/DashboardPage';
import { UserProfileView } from './userProfile/UserProfileView';
import { ApiKeysView } from './apiKeys/ApiKeysView';
import { AccountSecurityView } from './accountSecurity/AccountSecurityView';
import { PatsView } from './pats/PatsView';
import { NotificationsView } from 'tg.views/userSettings/notifications/NotificationsView';

export const UserSettingsRouter = () => {
  return (
    <DashboardPage>
      <Switch>
        <PrivateRoute exact path={LINKS.USER_PROFILE.template}>
          <UserProfileView />
        </PrivateRoute>

        <PrivateRoute
          exact
          path={[
            LINKS.USER_ACCOUNT_SECURITY.template,
            LINKS.USER_ACCOUNT_SECURITY_MFA_ENABLE.template,
            LINKS.USER_ACCOUNT_SECURITY_MFA_RECOVERY.template,
            LINKS.USER_ACCOUNT_SECURITY_MFA_DISABLE.template,
          ]}
        >
          <AccountSecurityView />
        </PrivateRoute>

        <PrivateRoute path={LINKS.USER_API_KEYS.template}>
          <ApiKeysView />
        </PrivateRoute>

        <PrivateRoute path={LINKS.USER_PATS.template}>
          <PatsView />
        </PrivateRoute>

        <PrivateRoute path={LINKS.USER_ACCOUNT_NOTIFICATIONS.template}>
          <NotificationsView />
        </PrivateRoute>
      </Switch>
    </DashboardPage>
  );
};
