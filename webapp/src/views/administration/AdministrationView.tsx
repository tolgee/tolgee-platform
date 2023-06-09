import { useState } from 'react';
import { Switch } from 'react-router-dom';

import { PrivateRoute } from 'tg.component/common/PrivateRoute';
import { LINKS } from 'tg.constants/links';
import { AdministrationOrganizations } from './AdministrationOrganizations';
import { AdministrationUsers } from './AdministrationUsers';
import { AdministrationEeLicenseView } from './AdministrationEeLicenseView';
import { AdministrationCloudPlansView } from './AdministrationCloudPlansView';
import { AdministrationCloudPlanEditView } from './AdministrationCloudPlanEditView';
import { AdministrationCloudPlanCreateView } from './AdministrationCloudPlanCreateView';

export const AdministrationView = () => {
  const [search, setSearch] = useState('');

  return (
    <>
      <Switch>
        <PrivateRoute exact path={LINKS.ADMINISTRATION_ORGANIZATIONS.template}>
          <AdministrationOrganizations search={search} setSearch={setSearch} />
        </PrivateRoute>
        <PrivateRoute exact path={LINKS.ADMINISTRATION_USERS.template}>
          <AdministrationUsers search={search} setSearch={setSearch} />
        </PrivateRoute>
        <PrivateRoute exact path={LINKS.ADMINISTRATION_EE_LICENSE.template}>
          <AdministrationEeLicenseView />
        </PrivateRoute>
        <PrivateRoute
          exact
          path={LINKS.ADMINISTRATION_BILLING_CLOUD_PLANS.template}
        >
          <AdministrationCloudPlansView />
        </PrivateRoute>
        <PrivateRoute
          exact
          path={LINKS.ADMINISTRATION_BILLING_CLOUD_PLAN_CREATE.template}
        >
          <AdministrationCloudPlanCreateView />
        </PrivateRoute>
        <PrivateRoute
          exact
          path={LINKS.ADMINISTRATION_BILLING_CLOUD_PLAN_EDIT.template}
        >
          <AdministrationCloudPlanEditView />
        </PrivateRoute>
      </Switch>
    </>
  );
};
