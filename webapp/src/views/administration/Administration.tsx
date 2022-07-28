import { PrivateRoute } from 'tg.component/common/PrivateRoute';
import { LINKS } from 'tg.constants/links';
import { Switch } from 'react-router-dom';
import { AdministrationOrganizations } from './AdministrationOrganizations';

export const Administration = () => (
  <>
    <Switch>
      <PrivateRoute exact path={LINKS.ADMINISTRATION_ORGANIZATIONS.template}>
        <AdministrationOrganizations />
      </PrivateRoute>
    </Switch>
  </>
);
