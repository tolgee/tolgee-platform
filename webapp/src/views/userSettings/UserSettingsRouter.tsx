import { Switch } from 'react-router-dom';

import { PrivateRoute } from 'tg.component/common/PrivateRoute';
import { LINKS } from 'tg.constants/links';

import { DashboardPage } from 'tg.component/layout/DashboardPage';
import { UserProfileView } from './userProfile/UserProfileView';
import { ApiKeysView } from './apiKeys/ApiKeysView';

export const UserSettingsRouter = () => {
  return (
    <DashboardPage>
      <Switch>
        <PrivateRoute exact path={LINKS.USER_PROFILE.template}>
          <UserProfileView />
        </PrivateRoute>

        <PrivateRoute path={LINKS.USER_API_KEYS.template}>
          <ApiKeysView />
        </PrivateRoute>
      </Switch>
    </DashboardPage>
  );
};
