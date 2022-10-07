import { PrivateRoute } from 'tg.component/common/PrivateRoute';
import { LINKS } from 'tg.constants/links';
import { Switch } from 'react-router-dom';
import { AdministrationOrganizations } from './AdministrationOrganizations';
import { AdministrationUsers } from './AdministrationUsers';
import { useState } from 'react';

export const Administration = () => {
  const [search, setSearch] = useState('');

  return (
    <>
      <Switch>
        <PrivateRoute exact path={LINKS.ADMINISTRATION_ORGANIZATIONS.build()}>
          <AdministrationOrganizations search={search} setSearch={setSearch} />
        </PrivateRoute>
        <PrivateRoute exact path={LINKS.ADMINISTRATION_USERS.build()}>
          <AdministrationUsers search={search} setSearch={setSearch} />
        </PrivateRoute>
      </Switch>
    </>
  );
};
