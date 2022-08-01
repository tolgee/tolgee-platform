import { PrivateRoute } from 'tg.component/common/PrivateRoute';
import { LINKS } from 'tg.constants/links';
import { Switch } from 'react-router-dom';
import { AdministrationOrganizations } from './AdministrationOrganizations';
import { AdministrationUsers } from './AdministrationUsers';

export const Administration = () => {
  return (
    <>
      <Switch>
        <PrivateRoute exact path={LINKS.ADMINISTRATION_ORGANIZATIONS.build()}>
          <AdministrationOrganizations />
        </PrivateRoute>
        <PrivateRoute exact path={LINKS.ADMINISTRATION_USERS.build()}>
          <AdministrationUsers />
        </PrivateRoute>
      </Switch>
    </>
  );
};
