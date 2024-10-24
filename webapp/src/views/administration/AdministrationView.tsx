import { Switch } from 'react-router-dom';

import { PrivateRoute } from 'tg.component/common/PrivateRoute';
import { LINKS } from 'tg.constants/links';
import { AdministrationOrganizations } from './AdministrationOrganizations';
import { AdministrationUsers } from './AdministrationUsers';
import { AdministrationEeLicenseView } from 'tg.ee/billing/administration/subscriptionPlans/AdministrationEeLicenseView';
import { AdministrationCloudPlansView } from 'tg.ee/billing/administration/subscriptionPlans/AdministrationCloudPlansView';
import { AdministrationCloudPlanEditView } from 'tg.ee/billing/administration/subscriptionPlans/AdministrationCloudPlanEditView';
import { AdministrationCloudPlanCreateView } from 'tg.ee/billing/administration/subscriptionPlans/AdministrationCloudPlanCreateView';
import { AdministrationEePlansView } from 'tg.ee/billing/administration/subscriptionPlans/AdministrationEePlansView';
import { AdministrationEePlanEditView } from 'tg.ee/billing/administration/subscriptionPlans/AdministrationEePlanEditView';
import { AdministrationEePlanCreateView } from 'tg.ee/billing/administration/subscriptionPlans/AdministrationEePlanCreateView';
import { useUrlSearchState } from 'tg.hooks/useUrlSearchState';
import { AdministrationEeTAView } from 'tg.ee/billing/administration/translationAgencies/AdministrationEeTAView';
import { AdministrationEeTACreateView } from 'tg.ee/billing/administration/translationAgencies/AdministrationEeTACreateView';
import { AdministrationEeTAEditView } from 'tg.ee/billing/administration/translationAgencies/AdministrationEeTAEditView';

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
        <PrivateRoute exact path={LINKS.ADMINISTRATION_EE_LICENSE.template}>
          <AdministrationEeLicenseView />
        </PrivateRoute>
        <PrivateRoute exact path={LINKS.ADMINISTRATION_EE_TA.template}>
          <AdministrationEeTAView />
        </PrivateRoute>
        <PrivateRoute exact path={LINKS.ADMINISTRATION_EE_TA_CREATE.template}>
          <AdministrationEeTACreateView />
        </PrivateRoute>
        <PrivateRoute exact path={LINKS.ADMINISTRATION_EE_TA_EDIT.template}>
          <AdministrationEeTAEditView />
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
        <PrivateRoute
          exact
          path={LINKS.ADMINISTRATION_BILLING_EE_PLANS.template}
        >
          <AdministrationEePlansView />
        </PrivateRoute>
        <PrivateRoute
          exact
          path={LINKS.ADMINISTRATION_BILLING_EE_PLAN_CREATE.template}
        >
          <AdministrationEePlanCreateView />
        </PrivateRoute>
        <PrivateRoute
          exact
          path={LINKS.ADMINISTRATION_BILLING_EE_PLAN_EDIT.template}
        >
          <AdministrationEePlanEditView />
        </PrivateRoute>
      </Switch>
    </>
  );
};
