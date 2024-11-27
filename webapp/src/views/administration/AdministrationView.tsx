import { Switch } from 'react-router-dom';

import { PrivateRoute } from 'tg.component/common/PrivateRoute';
import { LINKS } from 'tg.constants/links';
import { AdministrationOrganizations } from './AdministrationOrganizations';
import { AdministrationUsers } from './AdministrationUsers';

import { useUrlSearchState } from 'tg.hooks/useUrlSearchState';
import { getEe } from '../../plugin/getEe';

const {
  routes: { Administration: eeRoutes },
} = getEe();

export const AdministrationView = () => {
  const [search, setSearch] = useUrlSearchState('search');

  return (
    <>
      <Switch>
        <PrivateRoute exact path={LINKS.ADMINISTRATION_ORGANIZATIONS.template}>
          <AdministrationOrganizations
            search={search as string}
            setSearch={setSearch}
          />
        </PrivateRoute>
        <PrivateRoute exact path={LINKS.ADMINISTRATION_USERS.template}>
          <AdministrationUsers
            search={search as string}
            setSearch={setSearch}
          />
        </PrivateRoute>
        {eeRoutes}
      </Switch>
    </>
  );
};
