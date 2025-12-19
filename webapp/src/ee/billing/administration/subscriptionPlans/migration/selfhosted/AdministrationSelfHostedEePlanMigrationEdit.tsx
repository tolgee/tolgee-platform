import React, { useState } from 'react';
import { useTranslate } from '@tolgee/react';
import { LINKS, PARAMS } from 'tg.constants/links';
import { AdministrationPlanMigrationEditBase } from 'tg.ee.module/billing/administration/subscriptionPlans/migration/general/AdministrationPlanMigrationEditBase';
import { SelfHostedEePlanEditPlanMigrationForm } from 'tg.ee.module/billing/administration/subscriptionPlans/components/migration/SelfHostedEePlanEditPlanMigrationForm';
import {
  useBillingApiMutation,
  useBillingApiQuery,
} from 'tg.service/http/useQueryApi';
import { useHistory, useRouteMatch } from 'react-router-dom';

export const AdministrationSelfHostedEePlanMigrationEdit = () => {
  const { t } = useTranslate();
  const match = useRouteMatch();
  const [subscriptionsPage, setSubscriptionsPage] = useState(0);
  const [upcomingPage, setUpcomingPage] = useState(0);
  const migrationId = Number(match.params[PARAMS.PLAN_MIGRATION_ID]);
  const history = useHistory();

  if (isNaN(migrationId)) {
    history.replace(LINKS.ADMINISTRATION_BILLING_EE_PLANS.build());
  }

  const migrations = useBillingApiQuery({
    url: '/v2/administration/billing/self-hosted-ee-plans/migration/{migrationId}',
    method: 'get',
    path: { migrationId },
  });

  const subscriptions = useBillingApiQuery({
    url: '/v2/administration/billing/self-hosted-ee-plans/migration/{migrationId}/subscriptions',
    method: 'get',
    path: { migrationId },
    query: {
      page: subscriptionsPage,
      size: 10,
    },
    options: {
      keepPreviousData: true,
    },
  });

  const upcomingSubscriptions = useBillingApiQuery({
    url: '/v2/administration/billing/self-hosted-ee-plans/migration/{migrationId}/upcoming-subscriptions',
    method: 'get',
    path: { migrationId },
    query: {
      page: upcomingPage,
      size: 10,
    },
    options: {
      keepPreviousData: true,
    },
  });

  const toggleUpcomingSkip = useBillingApiMutation({
    url: '/v2/administration/billing/self-hosted-ee-plans/migration/{migrationId}/upcoming-subscriptions/{subscriptionId}/skip',
    method: 'put',
    invalidatePrefix:
      '/v2/administration/billing/self-hosted-ee-plans/migration/{migrationId}/upcoming-subscriptions',
  });

  const onToggleUpcomingSkip = (subscriptionId: number, skipped: boolean) => {
    toggleUpcomingSkip.mutate({
      path: { migrationId, subscriptionId },
      content: { 'application/json': { skipped } },
    });
  };

  return (
    <AdministrationPlanMigrationEditBase
      migrations={migrations}
      subscriptions={subscriptions}
      upcomingSubscriptions={upcomingSubscriptions}
      navigation={[
        [
          t('administration_ee_plans'),
          LINKS.ADMINISTRATION_BILLING_EE_PLANS.build(),
        ],
        [
          t('administration_plan_migration_configure_existing'),
          LINKS.ADMINISTRATION_BILLING_EE_PLAN_MIGRATION_EDIT.build({
            [PARAMS.PLAN_MIGRATION_ID]: migrationId,
          }),
        ],
      ]}
      listLink={LINKS.ADMINISTRATION_BILLING_EE_PLANS.build()}
      form={SelfHostedEePlanEditPlanMigrationForm}
      onPage={setSubscriptionsPage}
      onUpcomingPage={setUpcomingPage}
      onToggleUpcomingSkip={onToggleUpcomingSkip}
      upcomingToggleLoading={toggleUpcomingSkip.isLoading}
    />
  );
};
