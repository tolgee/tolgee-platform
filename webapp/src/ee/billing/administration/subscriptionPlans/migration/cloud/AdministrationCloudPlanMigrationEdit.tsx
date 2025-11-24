import React, { useState } from 'react';
import { useTranslate } from '@tolgee/react';
import { LINKS, PARAMS } from 'tg.constants/links';
import { AdministrationPlanMigrationEditBase } from 'tg.ee.module/billing/administration/subscriptionPlans/migration/general/AdministrationPlanMigrationEditBase';
import {
  useBillingApiMutation,
  useBillingApiQuery,
} from 'tg.service/http/useQueryApi';
import { useHistory, useRouteMatch } from 'react-router-dom';
import { CloudPlanEditPlanMigrationForm } from 'tg.ee.module/billing/administration/subscriptionPlans/components/migration/CloudPlanEditPlanMigrationForm';
import { components } from 'tg.service/billingApiSchema.generated';

type CloudPlanMigration = components['schemas']['CloudPlanMigrationModel'];

export const AdministrationCloudPlanMigrationEdit = () => {
  const { t } = useTranslate();
  const match = useRouteMatch();
  const migrationId = Number(match.params[PARAMS.PLAN_MIGRATION_ID]);
  const history = useHistory();

  if (isNaN(migrationId)) {
    history.replace(LINKS.ADMINISTRATION_BILLING_CLOUD_PLANS.build());
  }

  const [subscriptionsPage, setSubscriptionsPage] = useState(0);
  const [upcomingPage, setUpcomingPage] = useState(0);

  const migrations = useBillingApiQuery({
    url: '/v2/administration/billing/cloud-plans/migration/{migrationId}',
    method: 'get',
    path: { migrationId },
  });

  const subscriptions = useBillingApiQuery({
    url: '/v2/administration/billing/cloud-plans/migration/{migrationId}/subscriptions',
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
    url: '/v2/administration/billing/cloud-plans/migration/{migrationId}/upcoming-subscriptions',
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
    url: '/v2/administration/billing/cloud-plans/migration/{migrationId}/upcoming-subscriptions/{subscriptionId}/skip',
    method: 'put',
    invalidatePrefix:
      '/v2/administration/billing/cloud-plans/migration/{migrationId}/upcoming-subscriptions',
  });

  const onToggleUpcomingSkip = (subscriptionId: number, skipped: boolean) => {
    toggleUpcomingSkip.mutate({
      path: { migrationId, subscriptionId },
      content: { 'application/json': { skipped } },
    });
  };

  return (
    <AdministrationPlanMigrationEditBase<CloudPlanMigration>
      migrations={migrations}
      subscriptions={subscriptions}
      upcomingSubscriptions={upcomingSubscriptions}
      navigation={[
        [
          t('administration_cloud_plans'),
          LINKS.ADMINISTRATION_BILLING_CLOUD_PLANS.build(),
        ],
        [
          t('administration_plan_migration_configure_existing'),
          LINKS.ADMINISTRATION_BILLING_CLOUD_PLAN_MIGRATION_EDIT.build({
            [PARAMS.PLAN_MIGRATION_ID]: migrationId,
          }),
        ],
      ]}
      listLink={LINKS.ADMINISTRATION_BILLING_CLOUD_PLANS.build()}
      form={CloudPlanEditPlanMigrationForm}
      onPage={setSubscriptionsPage}
      onUpcomingPage={setUpcomingPage}
      onToggleUpcomingSkip={onToggleUpcomingSkip}
      upcomingToggleLoading={toggleUpcomingSkip.isLoading}
    />
  );
};
